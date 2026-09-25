package com.zaremate.keycheck;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class KeyCheckEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int LINES_PER_BATCH = 4;
    private static final Map<UUID, CheckSession> SESSIONS = new ConcurrentHashMap<>();

    private KeyCheckEvents() {}

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("keycheck")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player",
                                net.minecraft.commands.arguments.EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target =
                                            net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                    ServerPlayer initiator =
                                            ctx.getSource().getEntity() instanceof ServerPlayer p ? p : null;
                                    return startCheck(target, initiator);
                                }))
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Usage: /keycheck <player>"), false);
                            return 0;
                        })
        );
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CheckSession session = SESSIONS.remove(player.getUUID());
            if (session != null) restore(session);
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        int tick = event.getServer().getTickCount();
        for (CheckSession session : new ArrayList<>(SESSIONS.values())) {
            if (session.awaiting && tick >= session.timeoutTick)
                finish(session, "timeout");
        }
    }

    public static boolean isExpectedPacket(ServerPlayer player, ServerboundSignUpdatePacket packet) {
        CheckSession session = SESSIONS.get(player.getUUID());
        return session != null
                && session.awaiting
                && packet.isFrontText()
                && packet.getPos().equals(session.pos);
    }

    public static void handleSignResponse(ServerPlayer player, ServerboundSignUpdatePacket packet) {
        CheckSession session = SESSIONS.get(player.getUUID());
        if (!isExpectedPacket(player, packet)) return;

        session.awaiting = false;

        List<String> batch = session.keys.subList(
                session.index,
                Math.min(session.index + LINES_PER_BATCH, session.keys.size())
        );
        String[] lines = packet.getLines();

        for (int i = 0; i < batch.size() && i < lines.length; i++) {
            String response = lines[i] == null ? "" : lines[i].trim();
            if (response.equalsIgnoreCase(batch.get(i)))
                session.detected.add(batch.get(i));
        }

        restore(session);
        session.index += batch.size();

        if (session.index >= session.keys.size())
            finish(session, "complete");
        else
            sendBatch(session);
    }

    private static int startCheck(ServerPlayer target, ServerPlayer initiator) {
        if (SESSIONS.containsKey(target.getUUID())) {
            notify(initiator, "KeyCheck is already checking " + target.getGameProfile().getName() + ".");
            return 0;
        }

        List<String> keys = KeyCheckConfig.blacklistedKeys();
        if (keys.isEmpty()) {
            notify(initiator, "No blacklisted keys are configured.");
            return 0;
        }

        CheckSession session = new CheckSession(target, initiator, keys);
        SESSIONS.put(target.getUUID(), session);

        notify(initiator, "Checking " + target.getGameProfile().getName()
                + " for " + keys.size() + " blacklisted keybind(s).");
        sendBatch(session);
        return 1;
    }

    private static void sendBatch(CheckSession session) {
        ServerPlayer player = session.player;
        BlockPos pos = findAir(player);

        if (pos == null) {
            finish(session, "no suitable sign position");
            return;
        }

        session.pos = pos;
        session.originalState = player.serverLevel().getBlockState(pos);
        session.originalBlockEntity = player.serverLevel().getBlockEntity(pos);

        player.serverLevel().setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), 3);

        BlockEntity blockEntity = player.serverLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof SignBlockEntity sign)) {
            finish(session, "failed to create sign");
            return;
        }

        List<String> batch = session.keys.subList(
                session.index,
                Math.min(session.index + LINES_PER_BATCH, session.keys.size())
        );

        for (int i = 0; i < LINES_PER_BATCH; i++) {
            sign.setText(
                    i < batch.size() ? Component.keybind(batch.get(i)) : Component.empty(),
                    i
            );
        }
        sign.setChanged();

        player.connection.send(new ClientboundBlockEntityDataPacket(
                pos, sign.getType(), sign.getUpdateTag(player.registryAccess())));
        player.connection.send(new ClientboundOpenSignEditorPacket(pos, true));

        session.awaiting = true;
        session.timeoutTick =
                player.server.getTickCount() + KeyCheckConfig.TIMEOUT_TICKS.get();
    }

    private static void finish(CheckSession session, String reason) {
        if (session.finished) return;

        session.finished = true;
        SESSIONS.remove(session.player.getUUID());
        restore(session);

        String name = session.player.getGameProfile().getName();

        if (session.detected.isEmpty()) {
            if (KeyCheckConfig.LOG_CLEAN_CHECKS.get())
                LOGGER.info("[KeyCheck] {}: no blacklisted keybinds detected ({})", name, reason);
            notify(session.initiator,
                    "KeyCheck: " + name + " — no blacklisted keybinds detected.");
        } else {
            String list = String.join(", ", session.detected);
            LOGGER.warn("[KeyCheck] {}: detected {}", name, list);
            notify(session.initiator, "KeyCheck: " + name + " — DETECTED: " + list);
        }
    }

    private static void restore(CheckSession session) {
        if (session.pos == null) return;

        try {
            session.player.serverLevel().setBlock(
                    session.pos, session.originalState, 3);

            if (session.originalBlockEntity != null)
                session.player.serverLevel().setBlockEntity(session.originalBlockEntity);
        } catch (Exception e) {
            LOGGER.warn("[KeyCheck] Failed to restore sign at {}", session.pos, e);
        }

        session.pos = null;
    }

    private static BlockPos findAir(ServerPlayer player) {
        BlockPos base = player.blockPosition();

        for (int dy = 1; dy <= 5; dy++) {
            BlockPos pos = base.above(dy);
            if (player.serverLevel().getBlockState(pos).isAir())
                return pos;
        }

        return null;
    }

    private static void notify(ServerPlayer player, String message) {
        if (player != null)
            player.sendSystemMessage(Component.literal(message));
    }

    static final class CheckSession {
        final ServerPlayer player;
        final ServerPlayer initiator;
        final List<String> keys;
        final Set<String> detected = new LinkedHashSet<>();

        int index;
        int timeoutTick;
        BlockPos pos;
        BlockState originalState;
        BlockEntity originalBlockEntity;
        boolean awaiting;
        boolean finished;

        CheckSession(ServerPlayer player, ServerPlayer initiator, List<String> keys) {
            this.player = player;
            this.initiator = initiator;
            this.keys = List.copyOf(keys);
        }
    }
}
