package com.zaremate.keycheck;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignText;
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
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player",
                                net.minecraft.commands.arguments.EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target =
                                            net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                    ServerPlayer initiator =
                                            ctx.getSource().getEntity() instanceof ServerPlayer p ? p : null;
                                    return startCheck(target, initiator);
                                }))
        );
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CheckSession session = SESSIONS.remove(player.getUUID());
            if (session != null) finish(session, "logout");
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

        restoreClientView(session);
        session.index += batch.size();

        if (session.index >= session.keys.size())
            finish(session, "complete");
        else
            sendBatch(session);
    }

    private static int startCheck(ServerPlayer target, ServerPlayer initiator) {
        if (SESSIONS.containsKey(target.getUUID())) {
            LOGGER.info("[KeyCheck] {} is already being checked.", target.getGameProfile().getName());
            return 0;
        }

        List<String> keys = KeyCheckConfig.blacklistedKeys();
        if (keys.isEmpty()) {
            LOGGER.warn("[KeyCheck] No blacklisted keys are configured.");
            return 0;
        }

        CheckSession session = new CheckSession(target, initiator, keys);
        SESSIONS.put(target.getUUID(), session);

        LOGGER.info("[KeyCheck] Checking {} for {} blacklisted keybind(s).",
                target.getGameProfile().getName(), keys.size());

        sendBatch(session);
        return 1;
    }

    private static void sendBatch(CheckSession session) {
        ServerPlayer player = session.player;
        BlockPos pos = findAir(player);

        if (pos == null) {
            finish(session, "no suitable client-only sign position");
            return;
        }

        session.pos = pos;
        session.originalState = player.serverLevel().getBlockState(pos);
        session.originalBlockEntity = null;

        BlockState fakeSignState = Blocks.OAK_SIGN.defaultBlockState();
        SignBlockEntity sign = new SignBlockEntity(pos, fakeSignState);
        SignText text = new SignText();

        List<String> batch = session.keys.subList(
                session.index,
                Math.min(session.index + LINES_PER_BATCH, session.keys.size())
        );

        for (int i = 0; i < LINES_PER_BATCH; i++) {
            text = text.setMessage(
                    i,
                    i < batch.size() ? Component.keybind(batch.get(i)) : Component.empty()
            );
        }

        sign.setText(text, true);

        // These packets modify only the checking client's local world state.
        // The server world is never changed.
        player.connection.send(new ClientboundBlockUpdatePacket(pos, fakeSignState));
        player.connection.send(ClientboundBlockEntityDataPacket.create(sign));
        player.connection.send(new ClientboundOpenSignEditorPacket(pos, true));

        session.awaiting = true;
        session.timeoutTick =
                player.server.getTickCount() + KeyCheckConfig.TIMEOUT_TICKS.get();
    }

    private static void finish(CheckSession session, String reason) {
        if (session.finished) return;

        session.finished = true;
        SESSIONS.remove(session.player.getUUID());
        restoreClientView(session);

        String name = session.player.getGameProfile().getName();

        if ("complete".equals(reason)) {
            if (session.detected.isEmpty()) {
                if (KeyCheckConfig.LOG_CLEAN_CHECKS.get())
                    LOGGER.info("[KeyCheck] {}: no blacklisted keybinds detected.", name);
                DiscordWebhook.send(
                        name,
                        session.player.getUUID().toString(),
                        "CLEAN",
                        "No configured blacklisted keybinds were resolved."
                );
            } else {
                String list = String.join("\n", session.detected);
                LOGGER.warn("[KeyCheck] {}: detected blacklisted keybinds:\n{}", name, list);
                DiscordWebhook.send(
                        name,
                        session.player.getUUID().toString(),
                        "DETECTED",
                        "Detected keybinds:\n" + list
                );
            }
        } else {
            LOGGER.info("[KeyCheck] {}: check ended ({})", name, reason);
            DiscordWebhook.send(
                    name,
                    session.player.getUUID().toString(),
                    "INCONCLUSIVE",
                    "The client did not provide a usable response. Reason: " + reason
            );
        }
    }

    private static void restoreClientView(CheckSession session) {
        if (session.pos == null) return;

        // Restore only the checking client's local block view.
        session.player.connection.send(
                new ClientboundBlockUpdatePacket(session.pos, session.originalState)
        );

        session.pos = null;
    }

    private static BlockPos findAir(ServerPlayer player) {
        BlockPos base = player.blockPosition();

        for (int dy = 2; dy <= 6; dy++) {
            BlockPos pos = base.above(dy);
            if (player.serverLevel().getBlockState(pos).isAir())
                return pos;
        }

        return null;
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
