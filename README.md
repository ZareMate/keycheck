# KeyCheck

Server-side NeoForge 1.21.1 mod that probes client keybind translation keys.

## Command

`/keycheck <player>`

Requires permission level 2.

## Configuration

NeoForge generates:

`config/keycheck-common.toml`

Default:

```toml
blacklisted_keys = [
    "key.meteor-client.open-gui"
]
timeout_ticks = 60
log_clean_checks = true
```

Add one translation key per entry. The mod checks four probes per sign and automatically batches larger lists.

## Build

Requires Java 21 and Gradle. Run:

```bash
gradle build
```

## Detection

The check uses client-side keybind resolution and is heuristic. A modified client can suppress or spoof the response, so a positive result should be treated as a signal for further investigation.
