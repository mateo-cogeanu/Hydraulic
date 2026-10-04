# Mielon's The Sift on Fabric 26.3

This branch includes a **compatibility preview**, tested with [The Sift 1.1.2](https://modrinth.com/mod/mielons-the-sift), GeckoLib Fabric 5.5.7, Fabric API 0.161.0+26.3, Fabric Loader 0.19.5, and Java 25. It does **not yet provide full Bedrock support** for The Sift.

## Changes

The resource-format fixes apply to other mods too. Portal/effect adaptations and special entity proxies are specific to The Sift; Hydraulic has no mandatory dependency on The Sift or GeckoLib.

- Server block-state IDs are translated at the Geyser input boundary. Sift adds `ichorlogged` to vanilla blocks, shifting and expanding their IDs; matching vanilla states by name and known properties restores their original mappings. Chunk palettes, block updates, falling blocks, block particles/events, block-state entity metadata, and direct world reads use translated IDs. Registered custom states retain their IDs. Unconverted fluid states use a vanilla fluid presentation; unsupported mapping slots receive an air fallback.
- Negative Java block hardness now produces an effectively unbreakable Bedrock component while retaining Java's `-1` mining hardness. Previously Sift's portal caused an exception that stopped registration of later blocks.
- Item conversion follows `minecraft:item_model` and the model references in `assets/<namespace>/items`, including block-model paths. Static fallback models are selected for select/range definitions; inactive models are preferred for conditional definitions. Dynamic item appearances and composite item layers are not fully translated.
- Multipart block conditions support nested AND/OR, unconditional pieces, alternatives, and negation. All matching pieces are exported with independent rotations and material names. States with identical presentations share geometry. Weighted alternatives still select the first variant, and multipart UV-lock correction remains unsupported.
- Geyser receives each state's collision boxes as centers and sizes rather than an empty collision array. Complex Bedrock selection/collision boxes still use a bounding box, and rotated component boxes retain the existing rotation limitation.
- Armor conversion reads modern equipment files when the resource reader misses them, including files with additional equipment layer types. Attachables reference the current custom item identifier, bind by item identifier without an owner query, use slot-specific parent setup, and include explicit 3D humanoid geometry with the Java 64×32 UV layout. This covers the four Siftite pieces; it does not convert arbitrary Java armor renderer code.
- Single unchanged aliases to vanilla sound events use Geyser's sound mappings, including aliases reached through other mod events. They are not emitted as nonexistent OGG paths. Weighted mixtures, modified volume/pitch aliases, and aliases without a Geyser mapping need additional handling.
- Generated pack identities now include both the mod content and the Hydraulic converter build. Rebuilding an unchanged Sift mod with a new Hydraulic JAR changes the pack UUID, so Bedrock cannot silently reuse a previous generated pack. Unchanged builds keep stable identities across restarts. Generated packs declare Bedrock engine 1.26.0 to select current rendering definitions.
- Mod language files are loaded separately from Geyser's downloaded vanilla locales. Geyser's text translator resolves advancement names/descriptions, entity names and other mod translation keys, with an English fallback. English, Polish and Korean Sift assets are read on every startup, including cached-pack starts.
- Sift's invisible Java portal block receives a six-faced, full-block, opaque Bedrock cube using the mod's existing shaderpack fallback texture. It follows the Java block light emission and passes light through. It is a static approximation, not the original Java shader. The four solid portal structure block types receive explicit six-face cube geometry rather than depending on a built-in cube alias.
- Mod entity spawns are translated before MCProtocolLib decodes vanilla entity enums. Native UUIDs, entity IDs, position, motion and rotation are preserved; proxy metadata is restricted to compatible fields. Nine custom Bedrock appearances cover Singer, Echo Golem, Blub, Sifter, Dark Sniffer, Rift, Mini Rift and both willow boats. Ten entity types have protocol-safe spawn handling; Siftite Return currently uses a snowball appearance. The server retains all AI and interactions.
- Gecko geometry identifiers are rewritten; 28 animations are exported with Bedrock vector keyframes. Idle/movement animations are selected automatically. Special Java animation state, easing, callbacks, glow layers and variant/taming textures still need adaptation.
- Custom positional sounds are encoded by identifier rather than mod registry index; entity sounds are converted to positional sound packets and use Geyser's normal sound translator/mappings. This includes the eight Sonorous note sounds. The Sonorous note-block event emits the Sift note effect without triggering Bedrock's automatic vanilla instrument audio; the mod server sends the actual sound.
- Ten Sift particle definitions and their textures are exported, with expanding billboard sound waves. Server-sent mod particles within 24 blocks are sampled to at most eight effects per 250 milliseconds per player across all emitters (32 effects per second). The budget includes Sonorous note effects and is cleared on disconnect. Excess and distant effects are dropped before scheduling upstream packets. These approximate the Java effects; client-only emissions, full velocity/color logic and multi-frame texture playback are not translated.
- Event dispatch subscribes once per event type so additional modules do not duplicate item registrations.
- Materials are rebuilt from current assets, cyclic model parent walks terminate, and unresolved stitched models are excluded from the model converter.

## Verified

A dedicated Fabric server starts with Hydraulic, Geyser, Floodgate, The Sift, GeckoLib, and Fabric API. Sift's dimension loads and its resource pack is generated. The startup registers 71 custom blocks (including Geyser's built-in entries) and 91 custom items; Sift contributes 64 blocks with blockstate files and 90 items. The updated run has no registration errors or missing item-model warnings. The pack audit verifies 47 item icons, four armor attachables, 507 shared multipart geometries, and 20 local sound events; another 29 events use vanilla sound mappings. Thirty-five regression tests pass. The pack also contains nine entity appearances, 28 animations, ten particle effects, a full portal cube and four explicit armor geometries. A runtime Fabric smoke mod verifies ten native entity spawn/metadata wire round trips, 49 native sound wire round trips, nine event-bus custom appearance selections, armor item identifiers across Bedrock palettes, assigned geometry for all four frame block types, an entity sound packet, entity tracking cleanup, and an actual Geyser advancement translation ("Brave the Unknown"). Sift's main portal worldgen structure and NBT template were placed successfully in the Sift test dimension.

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

## Invisible visuals regression (2026-10-04)

The user reported that the first visual preview left entities, armor, the portal and its frame invisible, while Sonorous notes worked. The old pack UUID only depended on the unmodified Sift JAR, and its version stayed at 1.0.0 even after Hydraulic regenerated it. The new identity includes the converter build, and the manifest now uses engine 1.26.0 rather than 1.16.0. Armor uses default identifier binding; portal/frame geometry and entity sound routing have been revised. This identifies concrete defects and validates the corrected server-side paths, but the corrected rendering still needs the user's Bedrock test.

After replacing only Hydraulic and restarting, reconnect and accept the new resource pack download. There is no need to edit The Sift or rebuild the world. Old client packs have a different UUID from the newly generated one.

## Phone performance and portal volume (2026-10-04)

The user can join and play for roughly 20 seconds before the phone crashes; the portal now renders but its thin surface is incorrect. The revised portal uses a full 16×16×16 cube with the existing texture on all six faces. Portal and frame geometry use explicit directional culling rules so faces adjacent to full opaque blocks can be hidden. Collision and Java portal travel behavior remain server-controlled.

The previous bridge could enqueue up to 256 particle packets for each native emission with no shared per-player budget. The new bridge limits nearby mod effects to 32 per second per player, with a burst of eight, and drops emissions farther than 24 blocks. This reduces a concrete source of client and network load; it does not establish the phone crash's root cause. Entity models contain at most 16 cubes each and the unpacked generated Sift pack is roughly 18 MB. Mob models and audio remain enabled.

The full-block and culling definitions follow [Microsoft's block culling documentation](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/blockreference/examples/definitions/blockculling?view=minecraft-bedrock-stable). Local tests cover burst sharing, budget replenishment and the full portal bounds. Runtime testing verifies the culling field in Geyser's emitted block components. A sustained phone test is still required to confirm stability.

## Sustained phone slowdown diagnostic (2026-10-04)

The phone still slows and crashes after the prior particle-limit build. It initially appeared worse near the portal, but the user now reports lag even when looking away. The cause is unconfirmed; this build reduces the portal rendering path and isolates converted particle effects.

The portal uses `minecraft:geometry.full_block` with an opaque static 32×32 texture instead of custom portal geometry and the original 384×256 shader fallback. Java travel/collision logic is unchanged. Converted mod particles, including Sonorous note visuals, are disabled by default in this diagnostic build; note and entity sounds remain enabled, as do mob/armor models. Optional `-Dhydraulic.bedrock.mod-particles=true` before `-jar` re-enables budgeted non-portal mod particles. Portal parallax stays disabled. Vanilla particles are unaffected.

Thirty-seven regression tests pass. The pack audit checks the actual 32×32 PNG and absence of custom portal geometry; runtime checks verify built-in opaque portal components and frame culling. These are server/asset checks, not confirmation that the phone crash is solved. Comparison with Java clients, the Overworld and a sustained phone session is needed if lag remains.

## Ichor animation texture memory (2026-10-04)

Pack inspection found two 256×8192 Ichor flipbook textures, containing 32 frames of 256×256 pixels each. Their combined base RGBA size is 16 MiB before mipmaps or GPU copies. The Sift converter now caps flipbook frame dimensions to 32×32 while preserving frame count, order, alpha and the existing ten-tick animation timing. The two strips become 32×1024, reducing their base decoded footprint to 256 KiB combined (64 times smaller). The original Sift JAR is unchanged. This is a concrete reduction in texture load, not proof of the phone crash's cause.

Thirty-nine regression tests pass. The generated-pack audit requires both fluid strips to be 32×1024 with unchanged animation timing. The portal remains a small built-in full block, and converted mod particles remain disabled by default during crash diagnosis. Mob models, armor, notes and entity audio remain enabled. Sustained phone stability still needs verification.

## Sift terrain rendering on iPhone 13 mini (2026-10-04)

The user reports only a modest improvement after texture reduction and identifies an iPhone 13 mini as the target. A runtime audit found that the converter assigned blended transparency to every non-occluding block, including dense Sift vines/foliage and wood doors/trapdoors. Java light occlusion is not a texture transparency classification. This builds large parts of the dimension in the blended render pass.

The Sift policy now uses alpha-tested cutout rendering for those blocks and retains blending for Ichor glass, panes and cauldron fluid surfaces. Cross-shaped plants retain their existing single-sided cutout method. Other mods retain the existing policy. Dry healthy sculk receives the same explicit six-face geometry and culling as the portal's solid frame blocks, reducing buried terrain faces while preserving its texture. The mobile texture limits and diagnostic particle setting are retained.

Forty-three regression tests pass. Runtime validation requires every Sift block outside the three translucent exceptions to avoid blended materials, and checks the five solid cube block types. These changes address measured conversion defects; they do not confirm phone stability. Test sustained travel and dense vegetation as well as the portal. Device/version logs remain useful if crashes continue.

## Packet/decode diagnostics after supplied server log (2026-10-04)

The supplied log contains repeated out-of-range IDs in the earlier 12:25–12:41 sessions and packet-limit blocks. The later sessions starting at 13:27 and beyond no longer show those decode warnings. The latest 14:48 connection instead times out about 33 seconds after joining without a decoder exception in the provided excerpt. A timeout is not an iOS crash report, and the historical registry errors alone do not establish the present failure's cause.

This diagnostic build adds per-session native and Bedrock packet-type counts, native chunk payload bytes, decoder error counts and exact client version/protocol/dimension information. It emits a summary at most once every five seconds while packets are flowing. The first three decode failures per session include full stack traces. Counters do not retain packets or payload buffers, and are reset each interval. No packet limits are raised and rendering behavior is unchanged from the terrain-fix build.

Forty-five regression tests pass, including counter reset and bounded error reporting. The runtime smoke check verifies that the upstream/session adapter mixins apply. After testing, provide the newest log section containing `Hydraulic traffic:` and any `Hydraulic decode diagnostic:` entries, along with whether Minecraft closes to the iPhone home screen or stays open/freezes/disconnects. An iOS Minecraft crash report is needed to identify a native client crash if the app closes. Phone stability remains unverified.

## Mobile server view limit after traffic capture (2026-10-04)

The user's newest log identifies Bedrock 1.26.52, protocol 2193, and shows no decode errors in the captured Sift session. Its first five-second traffic window includes 473 native chunk packets and 6,633,944 native chunk payload bytes. Later windows include 1,132–1,384 Bedrock movement packets per five seconds and repeated chunk loads/unloads. These are concrete load observations, not proof of the client crash's cause; initial login/teleport traffic can include more than one chunk region.

Hydraulic now caps the Java client-information view-distance request to four chunks for iOS, Google Android and Amazon clients, including login defaults and subsequent radius changes. Smaller requests are preserved; other device platforms are unchanged. This asks the server to reduce streamed chunks and distant entity tracking. The phone's eight-chunk slider can remain set, but the Java server request is four. This does not change mob spawning, models, armor or sounds, and does not drop relative movement updates. Local mobs may still produce substantial traffic.

The startup JVM property `-Dhydraulic.bedrock.mobile-view-distance=4` controls the cap (bounded to 2–32); raising it increases client load. The five-second summaries now include `requestedView` and `javaViewRequest`, which describe the request, not a measurement of ServerCore's effective distance. Forty-nine regression tests pass. The unshipped runtime probe checks the injected Geyser method for every device OS, server login defaults and the initial two-chunk fallback. Phone stability and the traffic reduction still need a user retest, especially with the user's ServerCore/Lithium configuration. The Sift JAR and all five other shipped mods remain unchanged.

## Custom entity isolation after mobile retest (2026-10-04)

The next user log confirms the four-chunk request (`requestedView=8`, `javaViewRequest=4`), 117 initial native chunks versus 473 in the previous capture, and zero decoder errors. The user confirms Minecraft closes to the iPhone home screen. Later intervals still contain substantial mob traffic. The view cap reduced stream load but did not resolve the client failure. Server timeout records cannot identify the native client termination reason.

A new reversible setting in `config/hydraulic/config.yml`, `custom-entity-appearances: false`, isolates converted mob rendering. It defaults to true so existing compatibility remains available. Set false while the server is stopped, replace Hydraulic, restart and accept the new pack. The bridge still maps mod entity IDs to vanilla proxies and retains normal tracking, movement, collisions and server mob behavior; pigs/armor stands/sniffers/boats stand in for mod appearances. Custom entity definitions, geometry and animations are omitted from the generated pack in this mode. Entity sounds, armor, portal/terrain, and the mobile view cap are preserved. This is a diagnostic comparison, not the final appearance implementation or a verified crash fix.

Pack identity includes the mode, so toggling the setting regenerates/downloads the appropriate pack rather than reusing a cached custom appearance pack. Traffic summaries include `entityAppearance=vanilla-proxy` or `custom`, and startup logs identify isolation mode. Restore `custom-entity-appearances: true` and restart to restore models. Fifty regression tests pass, including cache separation for the two modes; live tests cover normal and proxy event-bus selection and native wire compatibility. The unmodified Sift mod remains unchanged.

For a client crash, share the iPhone's latest Minecraft diagnostic report (or matching JetsamEvent if present) from Settings → Privacy & Security → Analytics & Improvements → Analytics Data. The report can distinguish a memory termination from an application exception, while this rendering comparison narrows what triggers it. Apple describes diagnostic data access at https://www.apple.com/uk/legal/privacy/data/en/device-analytics/ .

## Current preview: dimension limits, restored entities/effects and portal sequence (2026-10-04)

This section supersedes the earlier diagnostic defaults. The user's Windows Bedrock test stays open but remains laggy; custom entity isolation did not resolve the lag. The matching iPhone JetsamEvent on October 4 at 14:37:13 identifies Minecraft as the frontmost process killed for `per-process-limit`, at approximately 2.20 GB resident memory. The supplied CPU-resource report recorded sustained CPU use but no termination action. The phone failure is therefore a confirmed memory termination in that session, while the content responsible for the memory/load remains unconfirmed.

Bedrock Java view requests are now capped at four chunks **only in `the_sift:the_sift`**, on every Bedrock platform including Windows. Entering or leaving a dimension resends Java client settings, restoring the client's requested distance in the Overworld, Nether and End. The existing JVM property `hydraulic.bedrock.mobile-view-distance` still adjusts this cap. It is a server request, not a guarantee of the effective distance with ServerCore. Java players are unaffected.

Converted effects are restored outside the Sift, including Sonorous note effects and portal parallax. The existing safety ceiling of 256 effects per native packet remains. Inside the Sift, mod particles share a 32-per-second budget, burst eight, range 24 blocks, and portal parallax is suppressed. Vanilla particles and all sounds remain enabled. Custom entity models remain enabled: upgrading configuration version 1 to 2 restores `custom-entity-appearances: true` once, ending the old isolation comparison while retaining other settings. Version 2 still accepts an explicit false for later diagnosis.

Singer native synchronized state now drives all seven exported animation phases: idle, walk, appear, sing, disappear, receive soul and barter hold. Rift and Mini Rift controllers play their appearance and reversed closing animation and hold their open pose. Native phase changes are forwarded immediately; elapsed time is synchronized at up to five Hz inside the Sift and interpolated by Bedrock. The original mod runs the opening event, structure placement, Singer spawning and audio. These controllers reproduce exported skeletal animations; Java-only glow outlines, dynamic head lights and held-soul render layers are not reproduced by this change.

The full-block portal uses a seamless periodic sky/star flipbook: 16 opaque 32×32 frames, three ticks per frame, decoded RGBA footprint 64 KiB. It emits light level 15, disables face dimming/ambient shading, and supplies an emissive texture-set value for compatible PBR rendering. This is an inexpensive animated approximation, not the Java portal shader's camera-dependent depth/parallax. Identical atlas tiles still repeat across blocks, but their cloud pattern continues across tile edges. Emissive bloom depends on the Bedrock graphics mode; ordinary rendering still gets block light and an undimmed material.

Validation: 56 regression tests pass; local fresh and cached startup checks pass; the live mixin fixture verifies four-chunk Sift requests and restored eight-chunk Overworld requests across every device OS, including login defaults. The original Sift 1.1.2 Singer/Rift classes successfully supply synchronized states, with Singer appear/sing/disappear timing boundaries verified. All 14,139 visible Sift state/palette lookups map to non-air blocks. Pack validation finds nine appearances, 32 animation definitions, three phase controllers, ten effects, four armor attachables and three bounded flipbooks. Native entity/sound wire compatibility and config migration also pass. These checks do not reproduce client rendering, explain missing collidable areas, or confirm sustained phone stability. Missing terrain needs a client location/block identification to distinguish unloaded sections from a specific rendering defect. Only Hydraulic is changed in the set; the Sift JAR remains unmodified.
