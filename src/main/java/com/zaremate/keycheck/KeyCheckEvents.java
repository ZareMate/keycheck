package com.zaremate.keycheck;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import com.zaremate.keycheck.mixin.SignBlockEntityAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class KeyCheckEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int LINES_PER_BATCH = 3;
    private static final String CTRL_KEYBIND = "key.forward";
    private static final Map<UUID, CheckSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PENDING_JOIN_CHECKS = new ConcurrentHashMap<>();
    private static final Map<UUID, ClientboundGameEventPacket> HELD_LOADING_PACKETS = new ConcurrentHashMap<>();
    private static final Set<UUID> RELEASING_LOADING_PACKETS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> FIRST_JOIN_CHECKED = ConcurrentHashMap.newKeySet();

    private KeyCheckEvents() {}

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("keycheck")
                        .requires(source -> source.hasPermission(3) || LuckPermsPermissions.hasPermission(source, KeyCheckConfig.COMMAND_PERMISSION.get()))
                        .then(Commands.argument("player",
                                net.minecraft.commands.arguments.EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target =
                                            net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                    return startCheck(target, ctx.getSource());
                                }))
        );
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!KeyCheckConfig.AUTO_CHECK_ON_JOIN.get()) return;
        if (LuckPermsPermissions.hasPermission(player, KeyCheckConfig.JOIN_BYPASS_PERMISSION.get())) {
            LOGGER.info("[KeyCheck] Skipping automatic join check for {} due to LuckPerms permission '{}'.",
                    player.getGameProfile().getName(), KeyCheckConfig.JOIN_BYPASS_PERMISSION.get());
            return;
        }
        UUID uuid = player.getUUID();
        if (HELD_LOADING_PACKETS.containsKey(uuid)) return;
        if (KeyCheckConfig.ONLY_FIRST_JOIN.get() && !FIRST_JOIN_CHECKED.add(uuid)) return;
        PENDING_JOIN_CHECKS.put(uuid,
                player.server.getTickCount() + KeyCheckConfig.JOIN_CHECK_DELAY_TICKS.get());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PENDING_JOIN_CHECKS.remove(player.getUUID());
            HELD_LOADING_PACKETS.remove(player.getUUID());
            RELEASING_LOADING_PACKETS.remove(player.getUUID());
            CheckSession session = SESSIONS.remove(player.getUUID());
            if (session != null) finish(session, "logout");
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        int tick = event.getServer().getTickCount();

        for (var entry : new ArrayList<>(PENDING_JOIN_CHECKS.entrySet())) {
            if (tick < entry.getValue()) continue;
            UUID uuid = entry.getKey();
            PENDING_JOIN_CHECKS.remove(uuid);
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player != null && player.isAlive() && !SESSIONS.containsKey(uuid)) {
                startCheck(player, null);
            } else if (player != null) {
                releaseLoadingScreen(player);
            }
        }

        for (CheckSession session : new ArrayList<>(SESSIONS.values())) {
            if (session.openTick > 0 && tick >= session.openTick) {
                session.openTick = 0;
                if (!session.finished && session.pos != null) {
                    session.player.connection.send(
                            new ClientboundOpenSignEditorPacket(session.pos, true));
                    // CheckHacks immediately clears the sign from the client's view
                    // after opening the editor; this does not touch the server world.
                    session.player.connection.send(
                            new ClientboundBlockUpdatePacket(
                                    session.pos, Blocks.AIR.defaultBlockState()));
                    session.awaiting = true;
                    session.timeoutTick = tick + KeyCheckConfig.TIMEOUT_TICKS.get();
                }
            }
            if (session.awaiting && tick >= session.timeoutTick)
                finish(session, "timeout");
        }
    }

    /**
     * Intercepts the client's initial chunk-load-start signal for automatic join checks.
     * The packet is sent only after the complete check has finished.
     */
    public static boolean holdLoadingScreen(
            net.minecraft.server.network.ServerGamePacketListenerImpl connection,
            ClientboundGameEventPacket packet) {
        if (packet.getEvent() != ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START)
            return false;
        if (!KeyCheckConfig.AUTO_CHECK_ON_JOIN.get())
            return false;

        ServerPlayer player = connection.getPlayer();
        if (player == null)
            return false;

        if (LuckPermsPermissions.hasPermission(player, KeyCheckConfig.JOIN_BYPASS_PERMISSION.get()))
            return false;

        UUID uuid = player.getUUID();
        if (KeyCheckConfig.ONLY_FIRST_JOIN.get() && FIRST_JOIN_CHECKED.contains(uuid))
            return false;

        FIRST_JOIN_CHECKED.add(uuid);
        ClientboundGameEventPacket previous = HELD_LOADING_PACKETS.putIfAbsent(uuid, packet);
        if (previous != null)
            return true;

        PENDING_JOIN_CHECKS.put(
                uuid,
                player.server.getTickCount() + KeyCheckConfig.JOIN_CHECK_DELAY_TICKS.get()
        );

        LOGGER.info(
                "[KeyCheck] Holding {} on the client loading screen until the join check completes.",
                player.getGameProfile().getName()
        );
        return true;
    }

    public static boolean isReleasingLoadingScreen(ServerPlayer player) {
        return RELEASING_LOADING_PACKETS.remove(player.getUUID());
    }

    private static void releaseLoadingScreen(ServerPlayer player) {
        ClientboundGameEventPacket packet = HELD_LOADING_PACKETS.remove(player.getUUID());
        if (packet == null)
            return;

        RELEASING_LOADING_PACKETS.add(player.getUUID());
        player.connection.send(packet);
        LOGGER.info(
                "[KeyCheck] Released {} from the client loading screen; KeyCheck is complete.",
                player.getGameProfile().getName()
        );
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

        List<KeyProbe> batch = session.probes.subList(
                session.index,
                Math.min(session.index + LINES_PER_BATCH, session.probes.size())
        );
        String[] lines = packet.getLines();

        String[] rawLines = Arrays.copyOf(lines, 4);
        for (int i = 0; i < rawLines.length; i++)
            if (rawLines[i] == null) rawLines[i] = "";

        String ctrlResponse = rawLines[3].trim();
        boolean exploitPreventer = ctrlResponse.equalsIgnoreCase(CTRL_KEYBIND);

        LOGGER.info(
                "[KeyCheck] Batch {} from {} L0='{}' L1='{}' L2='{}' CTRL='{}'{}",
                session.index / LINES_PER_BATCH,
                player.getGameProfile().getName(),
                rawLines[0].trim(),
                rawLines[1].trim(),
                rawLines[2].trim(),
                ctrlResponse,
                exploitPreventer ? " [ExploitPreventer detected]" : ""
        );

        for (int i = 0; i < batch.size(); i++) {
            String response = rawLines[i].trim();
            KeyProbe probe = batch.get(i);

            ProbeResult result = evaluate(probe, response, exploitPreventer);

            LOGGER.info(
                    "[KeyCheck] {} -> {} (mode={}, response='{}')",
                    probe.key(),
                    result,
                    probe.mode(),
                    response
            );

            if (result == ProbeResult.DETECTED)
                session.detected.add(probe.key());
            else if (result == ProbeResult.PROTECTED)
                session.protectedKeys.add(probe.key());
        }

        restoreClientView(session);
        session.index += batch.size();

        if (session.index >= session.probes.size())
            finish(session, "complete");
        else
            sendBatch(session);
    }

    private static int startCheck(ServerPlayer target, net.minecraft.commands.CommandSourceStack commandSource) {
        if (SESSIONS.containsKey(target.getUUID())) {
            if (commandSource != null) commandSource.sendFailure(Component.literal("KeyCheck is already checking " + target.getGameProfile().getName() + "."));
            LOGGER.info("[KeyCheck] {} is already being checked.", target.getGameProfile().getName());
            return 0;
        }

        List<KeyProbe> probes = KeyCheckConfig.blacklistedProbes();
        if (probes.isEmpty()) {
            if (commandSource != null) commandSource.sendFailure(Component.literal("No blacklisted keys are configured."));
            LOGGER.warn("[KeyCheck] No blacklisted keys are configured.");
            releaseLoadingScreen(target);
            return 0;
        }

        CheckSession session = new CheckSession(target, commandSource, probes);
        SESSIONS.put(target.getUUID(), session);

        LOGGER.info("[KeyCheck] Checking {} for {} configured probe(s).",
                target.getGameProfile().getName(), probes.size());

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

        // Packet serialization in 1.21.1 needs a non-null Level for registry access.
        // This only attaches the detached object to the existing Level; it does not
        // add the block entity to the world or cause a world update.
        sign.setLevel(player.serverLevel());

        SignText text = new SignText();

        List<KeyProbe> batch = session.probes.subList(
                session.index,
                Math.min(session.index + LINES_PER_BATCH, session.probes.size())
        );

        for (int i = 0; i < LINES_PER_BATCH; i++) {
            text = text.setMessage(
                    i,
                    i < batch.size() ? batch.get(i).component() : Component.empty()
            );
        }

        // CheckHacks reserves the fourth line for an ordinary keybind used
        // to identify clients that protect keybind translation.
        text = text.setMessage(3, Component.keybind(CTRL_KEYBIND));

        /*
         * Use the actual SignBlockEntity serializer so the NBT has the same
         * front_text/back_text structure as a normal Minecraft sign.
         */
        SignBlockEntityAccessor accessor = (SignBlockEntityAccessor) sign;
        accessor.keycheck$setFrontText(text);
        accessor.keycheck$setBackText(new SignText());
        accessor.keycheck$setEditor(player.getUUID());

        player.connection.send(new ClientboundBlockUpdatePacket(pos, fakeSignState));
        player.connection.send(ClientboundBlockEntityDataPacket.create(sign));

        // CheckHacks waits one tick after sending the sign data, then opens the
        // editor and immediately hides the sign from the checking client.
        session.openTick = player.server.getTickCount() + 1;
    }

    private enum ProbeResult { NOT_DETECTED, DETECTED, PROTECTED }

    private static ProbeResult evaluate(KeyProbe probe, String response, boolean exploitPreventer) {
        if (response.isEmpty())
            return ProbeResult.NOT_DETECTED;

        String key = probe.key();
        if (response.length() == key.length() + 1
                && response.regionMatches(true, 0, key, 0, key.length())
                && Character.isLetter(response.charAt(key.length()))) {
            return ProbeResult.NOT_DETECTED;
        }

        return switch (probe.mode()) {
            case METEOR -> {
                if (response.equalsIgnoreCase(key))
                    yield ProbeResult.DETECTED;
                if (response.regionMatches(true, 0, probe.fallback(), 0, probe.fallback().length()))
                    yield ProbeResult.NOT_DETECTED;
                yield ProbeResult.DETECTED;
            }
            case TRANSLATE -> {
                if (response.regionMatches(true, 0, probe.fallback(), 0, probe.fallback().length()))
                    yield ProbeResult.NOT_DETECTED;
                if (response.equalsIgnoreCase(key))
                    yield ProbeResult.PROTECTED;
                yield ProbeResult.DETECTED;
            }
            case KEYBIND -> {
                if (exploitPreventer && response.equalsIgnoreCase(key))
                    yield ProbeResult.PROTECTED;
                if (response.equalsIgnoreCase(key))
                    yield ProbeResult.NOT_DETECTED;
                yield ProbeResult.DETECTED;
            }
        };
    }

    private static void finish(CheckSession session, String reason) {
        if (session.finished) return;

        session.finished = true;
        SESSIONS.remove(session.player.getUUID());
        restoreClientView(session);
        releaseLoadingScreen(session.player);

        String name = session.player.getGameProfile().getName();

        if ("complete".equals(reason)) {
            if (!session.detected.isEmpty()) {
                StringBuilder details = new StringBuilder("Detected keybinds:\n")
                        .append(String.join("\n", session.detected));
                if (!session.protectedKeys.isEmpty()) {
                    details.append("\n\nProtected/probe-blocked:\n")
                            .append(String.join("\n", session.protectedKeys));
                }
                LOGGER.warn("[KeyCheck] {}: detected blacklisted keybinds:\n{}", name,
                        String.join("\n", session.detected));
                DiscordWebhook.send(
                        name,
                        session.player.getUUID().toString(),
                        "DETECTED",
                        details.toString()
                );
                sendCommandResult(session, "DETECTED", details.toString());
                broadcastStaff(session, name + " — DETECTED\n" + details);
            } else if (!session.protectedKeys.isEmpty()) {
                String list = String.join("\n", session.protectedKeys);
                LOGGER.info("[KeyCheck] {}: keybind probe protected for:\n{}", name, list);
                String details = "Protected/probe-blocked keybinds:\n" + list;
                DiscordWebhook.send(
                        name,
                        session.player.getUUID().toString(),
                        "INCONCLUSIVE",
                        details
                );
                sendCommandResult(session, "INCONCLUSIVE", details);
                broadcastStaff(session, name + " — INCONCLUSIVE\n" + details);
            } else {
                if (KeyCheckConfig.LOG_CLEAN_CHECKS.get())
                    LOGGER.info("[KeyCheck] {}: no blacklisted keybinds detected.", name);
                String details = "No configured blacklisted keybinds were resolved.";
                sendCommandResult(session, "CLEAN", details);
                // Clean checks are only logged/returned to the command sender; they are not broadcast.
                if (session.commandSource != null) {
                    DiscordWebhook.send(
                            name,
                            session.player.getUUID().toString(),
                            "CLEAN",
                            details
                    );
                }
            }
        } else {
            LOGGER.info("[KeyCheck] {}: check ended ({})", name, reason);
            String details = "The client did not provide a usable response. Reason: " + reason;
            DiscordWebhook.send(
                    name,
                    session.player.getUUID().toString(),
                    "INCONCLUSIVE",
                    details
            );
            sendCommandResult(session, "INCONCLUSIVE", details);
            broadcastStaff(session, name + " — INCONCLUSIVE\n" + details);
        }
    }

    private static void sendCommandResult(CheckSession session, String status, String details) {
        if (session.commandSource == null) return;
        session.commandSource.sendSuccess(
                () -> Component.literal("KeyCheck: " + session.player.getGameProfile().getName()
                        + " — " + status + " — " + details),
                false
        );
    }

    private static void broadcastStaff(CheckSession session, String message) {
        Component component = Component.literal("[KeyCheck] " + message);
        for (ServerPlayer player : session.player.server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(3)
                    || LuckPermsPermissions.hasPermission(player, KeyCheckConfig.BROADCAST_PERMISSION.get())) {
                player.sendSystemMessage(component);
            }
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
        final net.minecraft.commands.CommandSourceStack commandSource;
        final List<KeyProbe> probes;
        final Set<String> detected = new LinkedHashSet<>();
        final Set<String> protectedKeys = new LinkedHashSet<>();

        int index;
        int openTick;
        int timeoutTick;
        BlockPos pos;
        BlockState originalState;
        BlockEntity originalBlockEntity;
        boolean awaiting;
        boolean finished;

        CheckSession(ServerPlayer player, net.minecraft.commands.CommandSourceStack commandSource, List<KeyProbe> probes) {
            this.player = player;
            this.commandSource = commandSource;
            this.probes = List.copyOf(probes);
        }
    }
}
