# KeyCheck

Server-side NeoForge 1.21.1 mod that probes client keybind translation keys.

## Command

`/keycheck <player>`

Requires permission level 2.

The command does not kick, ban, damage, teleport, modify inventories, or otherwise punish the checked player.

## Discord webhook

Set these values in `config/keycheck-common.toml`:

```toml
webhook_enabled = true
webhook_url = "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"
```

The mod sends one Discord embed per completed or inconclusive check. The message contains the player name, UUID, result, and detected translation keys.

Keep the webhook URL private.

## Configuration

NeoForge generates:

`config/keycheck-common.toml`

Default:

```toml
blacklisted_keys = [
    "key.meteor-client.open-gui"
]

webhook_enabled = false
webhook_url = ""

timeout_ticks = 60
log_clean_checks = true
```

Add one translation key per entry. The mod checks four probes at a time and automatically batches larger lists.

## How the check works

The server sends a fake sign block, sign data containing `Component.keybind(...)`, and a sign-editor-open packet to the checked client.

The fake sign is never placed in the server world. Only the target client's local view is changed temporarily, and it is restored after the response.

The response packet is intercepted before normal vanilla sign handling, so the temporary sign is never saved or processed by the server.

The sign editor GUI may briefly appear on the checked client because that is part of the key-translation probe.

## Build

Requires Java 21 and Gradle:

```bash
gradle build
```

## Detection

This is a heuristic technique. A modified client can suppress or spoof keybind resolution, so a positive result should be treated as a signal for further investigation rather than absolute proof.
