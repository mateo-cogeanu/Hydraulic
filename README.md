# Hydraulic

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](https://discord.gg/geysermc)

Hydraulic is a companion to Geyser which allows for Bedrock players to join modded Minecraft: Java Edition servers. 

Hydraulic is an open collaboration project by [CubeCraft Games](https://cubecraft.net).

## What is Hydraulic?
Hydraulic is a server-side mod, which allows for Bedrock players to join modded Minecraft: Java Edition servers. This project works alongside [Geyser](https://github.com/GeyserMC/Geyser) to make this possible.

### This project is still in very early development and should not be used on production setups! You can get [Hydraulic](https://geysermc.org/download?project=other-projects&hydraulic=expanded) from the GeyserMC website.

## Contributing
Any contributions are appreciated. Please feel free to reach out to us on [Discord](https://discord.gg/geysermc) if
you're interested in helping out with Hydraulic.

### Project Setup
1. Clone the repo to your computer.
2. Navigate to the Hydraulic root directory and run `git submodule update --init --recursive`. This command downloads all the needed submodules for Hydraulic and is a crucial step in this process.
3. If your default JVM/JDK is not Java 25, please set your IDE to use a valid Java 25 JVM. Otherwise, you will run into an error while building Hydraulic. 
4. The project should import into your IDE after the loom setup is complete. For more detailed information, see the [Fabric setup](https://docs.fabricmc.net/develop/getting-started/setting-up).
5. Use `./gradlew build` to compile a jar file, or use `./gradlew :fabric:runServer` to run a server with Hydraulic installed. Make sure you have Geyser in your `mods` folder along with Hydraulic!

## Building this Minecraft 26.3 fork

Use Java 25. This branch targets Fabric Loader 0.19.5 or newer and Fabric API
0.161.0+26.3. Install `hydraulic-fabric.jar` with the matching Geyser-Fabric,
Floodgate-Fabric, and Fabric API JARs on a Minecraft 26.3 server.

Hydraulic compiles against the matching Geyser fork rather than the 26.2 release.
Check out `mateo-cogeanu/Geyser` on `codex/fabric-26.3` and run:

```sh
./gradlew :api:publishToMavenLocal :common:publishToMavenLocal :core:publishToMavenLocal :mod:publishToMavenLocal :fabric:publishToMavenLocal
```

Then run `./gradlew build` in this Hydraulic checkout. The local Geyser artifacts
use version `preview-codex-fabric-26.3-SNAPSHOT`.

Minecraft 26.3 tools and signs are classified through item tags. Custom axes,
hoes, shovels, and signs should belong to their corresponding vanilla item tags
to receive the matching Bedrock creative group.

## Links:
- Website: https://geysermc.org
- Docs: https://geysermc.org/wiki/other/hydraulic
- Download: https://geysermc.org/download?project=other-projects&hydraulic=expanded
- Discord: https://discord.gg/geysermc
- Donate: https://opencollective.com/geysermc

### The Sift compatibility preview

This branch adds block/item conversion fixes, explicit 3D armor, mod text translations, Sift portal/effect presentations and bridges for modded entities and sounds, tested with Mielon's The Sift 1.1.2 on Fabric 26.3. Full Bedrock support is still in progress. See [the compatibility report](docs/sift-compatibility.md) for validation and remaining special animations, fluids and client-rendering work.
