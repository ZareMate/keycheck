# KeyCheck

Server-side NeoForge 1.21.1 mod that probes client keybind/translation keys.

## Manual check

`/keycheck <player>`

Requires permission level 2.

## Probe types

Each entry in `blacklisted_keys` can define its own type:

```toml
blacklisted_keys = [
    "METEOR:key.meteor-client.open-gui",
    "KEYBIND:key.freecam.toggle",
    "TRANSLATE:litematica.hotkey.name.openmainmenuscreen"
]
```

Supported types:

- `KEYBIND` — uses `Component.keybind(...)`.
- `TRANSLATE` — uses `Component.translatableWithFallback(...)`.
- `METEOR` — uses the Meteor-style translation probe used by CheckHacks.

A bare entry such as `key.freecam.toggle` is still accepted and defaults to `KEYBIND`.

## Automatic join checks

```toml
auto_check_on_join = true
only_first_join = false
join_check_delay_ticks = 60
```

The default delay is 60 ticks (about 3 seconds), matching the original CheckHacks join-check timing.

## Discord webhook

```toml
webhook_enabled = true
webhook_url = "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"
```

Results are sent to Discord as embeds. Keep the webhook URL private.

## Enforcement

The mod does not kick, ban, damage, teleport, modify inventories, or execute punitive commands.

The probe uses temporary client-side packets and intercepts the sign response before normal server sign handling. The server world is not modified by the probe.

## Build

Java 21 + Gradle:

```bash
gradle build
```

Detection is heuristic because a modified client can suppress or spoof client-side resolution.
