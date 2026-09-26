# Airport Security System

Server-side NeoForge 1.21.1 mod that probes client keybind/translation keys.

## LuckPerms

Airport Security System uses LuckPerms for permission checks.

The default permissions are:

- `airport_security_system.command` — allows a player to use `/airport_security_system <player>`.
- `airport_security_system.join.bypass` — prevents that player from being automatically checked on join.

Both permission nodes can be changed in `config/airport_security_system-common.toml`:

```toml
command_permission = "airport_security_system.command"
join_bypass_permission = "airport_security_system.join.bypass"
```

Example LuckPerms setup:

```
/lp group admin permission set airport_security_system.command true
/lp group admin permission set airport_security_system.join.bypass true
```

The server console can run `/airport_security_system` without a player permission check.

LuckPerms must be installed on the NeoForge server for player permission checks. The mod compiles against the LuckPerms 5.5 API.

## Manual check

```
/airport_security_system <player>
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
join_check_delay_ticks = 40
```

Automatic checks wait 2 seconds (40 ticks by default) after login before the client probe starts. The delay can be changed in airport_security_system-common.toml.

The default per-batch response timeout is 120 ticks (6 seconds).

Players with `airport_security_system.join.bypass` are skipped from automatic join checks.

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

The mod includes an optional client-side verification overlay. On NeoForge clients with the Airport Security System mod installed, the server now starts a small configuration-phase handshake so the Airport Security System panel appears during the loading/configuration sequence before the world is shown. If the server-side sign probe is still running after login, the client keeps a blocking Airport Security System loading screen open until the check finishes and then returns to normal gameplay.

The detection logic remains server-side. The Airport Security System mod must also be installed on the client for the graphical overlay to appear; clients without it can still connect because the status payload is optional.
