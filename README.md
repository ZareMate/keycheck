# KeyCheck

Server-side NeoForge 1.21.1 mod that probes client keybind translation keys.

## Manual check

`/keycheck <player>`

Requires permission level 2.

## Automatic join checks

Players can be checked automatically shortly after joining, similar to CheckHacks.

Configuration:

```toml
auto_check_on_join = true
only_first_join = false
join_check_delay_ticks = 60
```

- `auto_check_on_join`: run a check after login.
- `only_first_join`: only automatically check each UUID once per server runtime.
- `join_check_delay_ticks`: delay after login; 60 ticks is about 3 seconds.

The automatic check uses the configured `blacklisted_keys` list.

## Discord webhook

```toml
webhook_enabled = true
webhook_url = "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"
```

One Discord embed is sent for each completed or inconclusive check. Keep the webhook URL private.

## Full default configuration

```toml
blacklisted_keys = [
    "key.meteor-client.open-gui"
]

auto_check_on_join = true
only_first_join = false
join_check_delay_ticks = 60

webhook_enabled = false
webhook_url = ""

timeout_ticks = 60
log_clean_checks = true
```

## Enforcement

This mod performs no kick, ban, damage, teleport, inventory modification, or other punitive action when a key is detected. Results are logged and can be sent to Discord.

The key-translation method is heuristic. A modified client can suppress or spoof the response. The probe itself uses a temporary client-side sign view, because opening the sign editor is part of the key-translation technique.

## Build

Requires Java 21 and Gradle:

```bash
gradle build
```
