# KeyCheck

Server-side NeoForge 1.21.1 mod that probes client-side sign text resolution.

## Command

`/keycheck <player>`

Requires permission level 2.

The checker does not kick, ban, damage, teleport, change inventories, or otherwise punish the player.

## Discord

Set:

```toml
webhook_enabled = true
webhook_url = "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"
```

Results are sent to Discord as CLEAN, DETECTED, or INCONCLUSIVE.

## Probe configuration

```toml
blacklisted_keys = [
    "key.meteor-client.open-gui",
    "xray.config.toggle"
]
```

Detection mode can be specified explicitly:

```toml
blacklisted_keys = [
    "METEOR:key.meteor-client.open-gui",
    "KEYBIND:xray.config.toggle",
    "TRANSLATE:some.translation.key"
]
```

For backwards compatibility, `key.meteor-client.open-gui` is automatically treated as METEOR. Other entries default to KEYBIND.

The original CheckHacks configuration uses `xray.config.toggle` as its XRay KEYBIND probe.

## Sign behavior

The sign probe follows the CheckHacks layout:

- lines 1-3: three configured probes
- line 4: `key.forward` control probe

For METEOR/TRANSLATE probes the sign uses a translation component with a fallback text. For KEYBIND probes it uses a keybind component.

The server sends the fake sign data first, waits one tick, then sends OPEN SIGN EDITOR and immediately sends a client-only AIR block update. No sign is placed in the server world.

The sign update packet is intercepted before normal vanilla sign processing.

## Build

Requires Java 21 and Gradle:

```bash
gradle build
```

Detection is heuristic because a modified client can suppress or spoof client-side text resolution.
