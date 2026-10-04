# Mielon's The Sift on Fabric 26.3

This branch includes a **compatibility preview**, tested with [The Sift 1.1.2](https://modrinth.com/mod/mielons-the-sift), GeckoLib Fabric 5.5.7, Fabric API 0.161.0+26.3, Fabric Loader 0.19.5, and Java 25. It does **not yet provide full Bedrock support** for The Sift.

## Changes

The resource-format fixes apply to other mods too. Portal/effect adaptations and special entity proxies are specific to The Sift; Hydraulic has no mandatory dependency on The Sift or GeckoLib.

- Server block-state IDs are translated at the Geyser input boundary. Sift adds `ichorlogged` to vanilla blocks, shifting and expanding their IDs; matching vanilla states by name and known properties restores their original mappings. Chunk palettes, block updates, falling blocks, block particles/events, block-state entity metadata, and direct world reads use translated IDs. Registered custom states retain their IDs. Unconverted fluid states use a vanilla fluid presentation; unsupported mapping slots receive an air fallback.
- Negative Java block hardness now produces an effectively unbreakable Bedrock component while retaining Java's `-1` mining hardness. Previously Sift's portal caused an exception that stopped registration of later blocks.
- Item conversion follows `minecraft:item_model` and the model references in `assets/<namespace>/items`, including block-model paths. Static fallback models are selected for select/range definitions; inactive models are preferred for conditional definitions. Dynamic item appearances and composite item layers are not fully translated.
- Multipart block conditions support nested AND/OR, unconditional pieces, alternatives, and negation. All matching pieces are exported with independent rotations and material names. States with identical presentations share geometry. Weighted alternatives still select the first variant, and multipart UV-lock correction remains unsupported.
- Geyser receives each state's collision boxes as centers and sizes rather than an empty collision array. Complex Bedrock selection/collision boxes still use a bounding box, and rotated component boxes retain the existing rotation limitation.
- Armor conversion reads modern equipment files when the resource reader misses them, including files with additional equipment layer types. Attachables reference the current custom item identifier, use slot-specific parent setup, and include explicit 3D humanoid geometry with the Java 64×32 UV layout. This covers the four Siftite pieces; it does not convert arbitrary Java armor renderer code.
- Single unchanged aliases to vanilla sound events use Geyser's sound mappings, including aliases reached through other mod events. They are not emitted as nonexistent OGG paths. Weighted mixtures, modified volume/pitch aliases, and aliases without a Geyser mapping need additional handling.
- Mod language files are loaded separately from Geyser's downloaded vanilla locales. Geyser's text translator resolves advancement names/descriptions, entity names and other mod translation keys, with an English fallback. English, Polish and Korean Sift assets are read on every startup, including cached-pack starts.
- Sift's invisible Java portal block receives a two-sided, axis-aware Bedrock surface using the mod's existing shaderpack fallback texture. It is a static approximation, not the original Java shader.
- Mod entity spawns are translated before MCProtocolLib decodes vanilla entity enums. Native UUIDs, entity IDs, position, motion and rotation are preserved; proxy metadata is restricted to compatible fields. Nine custom Bedrock appearances cover Singer, Echo Golem, Blub, Sifter, Dark Sniffer, Rift, Mini Rift and both willow boats. Ten entity types have protocol-safe spawn handling; Siftite Return currently uses a snowball appearance. The server retains all AI and interactions.
- Gecko geometry identifiers are rewritten; 28 animations are exported with Bedrock vector keyframes. Idle/movement animations are selected automatically. Special Java animation state, easing, callbacks, glow layers and variant/taming textures still need adaptation.
- Custom positional sounds are encoded by identifier rather than mod registry index; entity sounds use Geyser's sound mapping. This includes the eight Sonorous note sounds. The Sonorous note-block event emits the Sift note effect without triggering Bedrock's automatic vanilla instrument audio; the mod server sends the actual sound.
- Ten Sift particle definitions and their textures are exported, with expanding billboard sound waves. Server-sent mod particles are bridged to Bedrock with their positions/counts. These approximate the Java effects; client-only emissions, full velocity/color logic and multi-frame texture playback are not translated.
- Event dispatch subscribes once per event type so additional modules do not duplicate item registrations.
- Materials are rebuilt from current assets, cyclic model parent walks terminate, and unresolved stitched models are excluded from the model converter.

## Verified

A dedicated Fabric server starts with Hydraulic, Geyser, Floodgate, The Sift, GeckoLib, and Fabric API. Sift's dimension loads and its resource pack is generated. The startup registers 71 custom blocks (including Geyser's built-in entries) and 91 custom items; Sift contributes 64 blocks with blockstate files and 90 items. The updated run has no registration errors or missing item-model warnings. The pack audit verifies 47 item icons, four armor attachables, 507 shared multipart geometries, and 20 local sound events; another 29 events use vanilla sound mappings. Twenty-eight regression tests pass. The pack also contains nine entity appearances, 28 animations, ten particle effects, a portal surface and four explicit armor geometries. A runtime Fabric smoke mod verifies ten native entity spawn/metadata wire round trips, 49 native sound wire round trips, entity tracking cleanup, and an actual Geyser advancement translation ("Brave the Unknown"). Sift's main portal worldgen structure and NBT template were placed successfully in the Sift test dimension.

Regression tests cover block conditions, item references, equipment layer selection, sound event aliases, state identity projection, all block palette types, block packets/metadata, and sparse mapping tables:

```sh
./gradlew :fabric:build :shared:test
python3 tools/verify-sift-pack.py /path/to/server/config/hydraulic/storage/the_sift/the_sift.mcpack
```

The pack audit checks JSON validity, item/armor/entity/particle texture paths, geometry and animation references, multipart bone parenting/material names and geometry reuse, and local sound file paths. It does not prove Bedrock rendering or gameplay. The user reports that the previous chunk fix substantially improves gameplay and the Sift dimension loads. This new visual/entity/audio build still needs a Bedrock client test. Structure generation remains on the Java server; its blocks and entities travel through the same conversion bridges.

## Work remaining before full support

- Complete special mob animations, glow layers, variant/taming textures, native Siftite Return rendering, boat rowing/seat alignment and interaction testing. Basic spawn/render bridges do not prove every entity behavior.
- Translate Ichor's custom appearance and full fluid behavior. Its 16 states currently use corresponding vanilla water states because it has no standard blockstate JSON. Ichor logging on vanilla blocks is not represented faithfully.
- Improve portal animation/parallax beyond the static Bedrock surface.
- Adapt client-only dimension sky, weather, shaders, particles, custom networking and entity animation state. Java client code does not execute on Bedrock.
- Validate progression, portal activation, dimension travel, mob interaction, crafting/smithing, equipment, inventories and audio with an actual Bedrock client.

Use a separate test server/world for this preview. Install the Fabric GeckoLib artifact, not the NeoForge artifact linked by some dependency metadata.

## Chunk-error regression (2026-10-04)

The first user Bedrock test reported repeated `BlockMappings.getBedrockBlockId` null-pointer exceptions while loading chunks. A local registry audit reproduced missing mappings and showed that Sift also changes vanilla state IDs. The patch maps 62,323 shifted/expanded vanilla server states with zero unmatched vanilla states and maps 16 otherwise unconverted fluid states. Every server state was passed through the same Bedrock block-ID lookup used by chunk translation for all three supported palettes. Startup and pack audits pass. A separate server with the Hydraulic sample mod and no Sift also starts successfully, with zero shifted or unmatched vanilla states. The user subsequently confirmed improved behavior and successful dimension loading.

## Runtime bridge smoke test

`tools/bridge-smoke/BridgeSmoke.java` is the source for an **unshipped test mod**, compiled against the local Fabric/Minecraft/Hydraulic/Geyser dependencies and installed only on the test server. It runs on the first server tick after Geyser initialization, encodes native Minecraft packets and decodes them with MCProtocolLib. It checks that entity IDs/UUIDs and sound identifiers/volume survive the bridge, incompatible entity metadata is removed, and Geyser itself resolves an advancement title. It is excluded from all user download sets. The test is not a Bedrock rendering test.

The explicit armor layout and entity/particle presentations follow the [official attachable documentation](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/attachablereference/examples/attachabledefinitions/attachable) and [Mojang Bedrock samples](https://github.com/Mojang/bedrock-samples).
