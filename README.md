# KeyCheck

Server-side NeoForge 1.21.1 mod that probes client keybind/translation keys.

## LuckPerms

KeyCheck uses LuckPerms for permission checks.

The default permissions are:

- `keycheck.command` — allows a player to use `/keycheck <player>`.
- `keycheck.join.bypass` — prevents that player from being automatically checked on join.

Both permission nodes can be changed in `config/keycheck-common.toml`:

```toml
command_permission = "keycheck.command"
join_bypass_permission = "keycheck.join.bypass"
```

Example LuckPerms setup:

```
/lp group admin permission set keycheck.command true
/lp group admin permission set keycheck.join.bypass true
```

The server console can run `/keycheck` without a player permission check.

LuckPerms must be installed on the NeoForge server for player permission checks. The mod compiles against the LuckPerms 5.5 API.

## Manual check

```
/keycheck <player>
```

The result is returned to the command sender and sent to Discord when the webhook is enabled.

## Probe types

Each entry in `blacklisted_keys` can define its own type:

```toml
blacklisted_keys = [
    "METEOR:key.meteor-client.open-gui",
    "KEYBIND:key.freecam.toggle",
    "TRANSLATE:litematica.hotkey.name.openmainmenuscreen"
]
```

Supported types are `KEYBIND`, `TRANSLATE`, and `METEOR`.

A bare key is accepted for backwards compatibility.

## Automatic join checks

```toml
auto_check_on_join = true
only_first_join = false
join_check_delay_ticks = 60
```

Players with `keycheck.join.bypass` are skipped from automatic join checks.

## Discord webhook

```toml
webhook_enabled = true
webhook_url = "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"
```

Automatic clean checks do not generate a Discord message. Detected and inconclusive automatic results are sent.

Manual checks send their final result to the command sender. Manual clean checks also send their result to Discord.

## Enforcement

The mod does not kick, ban, damage, teleport, modify inventories, or execute punitive commands.

The probe uses temporary client-side packets and intercepts the sign response before normal server sign handling. The server world is not modified by the probe.

## Build

Requires Java 21 and Gradle:

```bash
gradle build
```

Detection is heuristic because a modified client can suppress or spoof client-side resolution.


## Client overlay

The mod includes an optional client-side verification overlay. On NeoForge clients with the KeyCheck mod installed, the server now starts a small configuration-phase handshake so the KeyCheck panel appears during the loading/configuration sequence before the world is shown. If the server-side sign probe is still running after login, the client keeps a blocking KeyCheck loading screen open until the check finishes and then returns to normal gameplay.

The detection logic remains server-side. The KeyCheck mod must also be installed on the client for the graphical overlay to appear; clients without it can still connect because the status payload is optional.
