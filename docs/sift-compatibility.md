# Mielon's The Sift on Fabric 26.3

This branch includes a **compatibility preview**, tested with [The Sift 1.1.2](https://modrinth.com/mod/mielons-the-sift), GeckoLib Fabric 5.5.7, Fabric API 0.161.0+26.3, Fabric Loader 0.19.5, and Java 25. It does **not yet provide full Bedrock support** for The Sift.

## Changes

The changes apply to other mods using the same resource formats; Hydraulic has no mandatory dependency on The Sift or GeckoLib.

- Negative Java block hardness now produces an effectively unbreakable Bedrock component while retaining Java's `-1` mining hardness. Previously Sift's portal caused an exception that stopped registration of later blocks.
- Item conversion follows `minecraft:item_model` and the model references in `assets/<namespace>/items`, including block-model paths. Static fallback models are selected for select/range definitions; inactive models are preferred for conditional definitions. Dynamic item appearances and composite item layers are not fully translated.
- Multipart block conditions support nested AND/OR, unconditional pieces, alternatives, and negation. All matching pieces are exported with independent rotations and material names. States with identical presentations share geometry. Weighted alternatives still select the first variant, and multipart UV-lock correction remains unsupported.
- Geyser receives each state's collision boxes as centers and sizes rather than an empty collision array. Complex Bedrock selection/collision boxes still use a bounding box, and rotated component boxes retain the existing rotation limitation.
- Armor conversion reads modern equipment files when the resource reader misses them, including files with additional equipment layer types. Attachables reference the current custom item identifier.
- Single unchanged aliases to vanilla sound events use Geyser's sound mappings, including aliases reached through other mod events. They are not emitted as nonexistent OGG paths. Weighted mixtures, modified volume/pitch aliases, and aliases without a Geyser mapping need additional handling.
- Event dispatch subscribes once per event type so additional modules do not duplicate item registrations.
- Materials are rebuilt from current assets, cyclic model parent walks terminate, and unresolved stitched models are excluded from the model converter.

## Verified

A dedicated Fabric server starts with Hydraulic, Geyser, Floodgate, The Sift, GeckoLib, and Fabric API. Sift's dimension loads and its resource pack is generated. The startup registers 71 custom blocks (including Geyser's built-in entries) and 91 custom items; Sift contributes 64 blocks with blockstate files and 90 items. The updated run has no registration errors or missing item-model warnings. The pack audit verifies 47 item icons, four armor attachables, 507 shared multipart geometries, and 20 local sound events; another 29 events use vanilla sound mappings. Ten regression tests pass.

Regression tests cover block conditions, item references, equipment layer selection, and sound event aliases:

```sh
./gradlew :fabric:build :shared:test
python3 tools/verify-sift-pack.py /path/to/server/config/hydraulic/storage/the_sift/the_sift.mcpack
```

The pack audit checks JSON validity, item texture paths, four armor attachables, multipart bone parenting/material names and geometry reuse, and local sound file paths. It does not prove Bedrock rendering or gameplay. A Bedrock player has not yet completed an end-to-end test.

## Work remaining before full support

- Translate modded Java entity types and metadata through Geyser, then export GeckoLib models, animations, textures and client entity definitions. The custom mobs, boats and rifts are not supported by these asset fixes.
- Translate Ichor's fluid states and behavior. It has no blockstate JSON, so it is not covered by standard block conversion.
- Provide a Bedrock portal renderer. Sift's portal model only supplies a particle texture; the visible portal is rendered by Java client code and remains invisible with the current empty geometry fallback.
- Adapt client-only dimension sky, weather, shaders, particles, custom networking and entity animation state. Java client code does not execute on Bedrock.
- Validate progression, portal activation, dimension travel, mob interaction, crafting/smithing, equipment, inventories and audio with an actual Bedrock client.

Use a separate test server/world for this preview. Install the Fabric GeckoLib artifact, not the NeoForge artifact linked by some dependency metadata.
