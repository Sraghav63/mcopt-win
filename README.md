# mcopt

A Minecraft performance mod for Windows 11 and Apple Silicon Macs. **Very much alpha:** expect bugs, and back up your worlds.

## Windows 11

The Windows port keeps the portable chunk, CPU, memory and startup optimizations and uses Windows thread scheduling.
Minecraft/Sodium provides the graphics backend. **Matching the Mac performance gains is not established:** Apple's
Metal renderer, MetalFX/frame generation, native noise and mcopt far terrain have no Windows equivalents in this port.
Windows gameplay and hardware benchmarks still need validation.

Install `dist/mcopt-windows-0.2.0-alpha.2.jar` alongside the Sodium version below in `%APPDATA%\.minecraft\mods`
(or your launcher's instance mods folder). Requires 64-bit Java 25+, Minecraft 26.3 and Fabric Loader 0.19.5+.
The FPS HUD is included. Remove any previous mcopt or separate mcopt-fps jars first.

Build on Windows with JDK 25: `powershell -ExecutionPolicy Bypass -File tools/release-windows.ps1`, or:

```powershell
.\gradlew.bat -PtargetPlatform=windows :metal:build :fpshud:build :metal:windowsRelease
```

Cross-build on Mac/Linux using `./gradlew` with the same arguments. Both the jar and a zip with installation instructions
are written to `dist/`. No Xcode, clang, Python or Apple frameworks are needed for this build.

Windows defaults to `profile=windows`. Its larger chunk cache is enabled only with at least 16 GB system memory and
2 GB Java heap. Config and JVM overrides for portable optimizations still take precedence; Apple-only hooks stay off.
`mcopt.qos=off` disables thread scheduling. Java may require `--enable-native-access=ALL-UNNAMED` for native scheduling;
if unavailable, mcopt logs once and keeps default Windows priorities.

See [WINDOWS-README.txt](WINDOWS-README.txt) for installation and a controlled benchmark procedure. The table below
contains Mac results only. `profile=none` disables profile settings; QoS and class preload have separate switches.

## Mac requirements

- An Apple Silicon Mac on macOS 26 or later
- Java 25
- Minecraft 26.3 with Fabric Loader 0.19.5
- Sodium 0.9.3, a separate download:
  [sodium-fabric-0.9.3-alpha.1+mc26.3.jar](https://cdn.modrinth.com/data/AANobbMI/versions/v4PSXean/sodium-fabric-0.9.3-alpha.1%2Bmc26.3.jar)

## Install

Drop the mcopt jar and the Sodium jar into `~/Library/Application Support/minecraft/mods`. Done.

Shows an fps counter in the top-left corner (hidden with F1 or F3).

## Settings

- The perf profile is on by default and picks its settings for your Mac.
- `profile=none` in `config/mcopt.properties` turns it off. The first launch writes that file.
- Far terrain is an experimental opt-in: `mcopt.lod=true` in the same file, for Macs with 10 or more GPU cores.

## Numbers

fps spinning / flying, same test world, 1920x1080, render distance 16, VSync off, two runs each:

| Mac | previous build | 0.2.0-alpha.1 |
|---|---|---|
| Mac mini M4 (10-core GPU, 16 GB) | 1094-1103 / 977-989 | 1072-1119 / 994-1002 |
| MacBook Neo (A18 Pro, 5-core GPU, 8 GB) | 379-449 / 324-339 | 460-471 / 415-432 |

## Known issues

- Only Sodium 0.9.3 works. With another version, Fabric stops at launch and says which one to install.
- Mods that call OpenGL directly can't draw on the Metal backend. `-Dmcopt.metal=false` switches mcopt's renderer off.
- Distant Horizons only works on OpenGL. mcopt detects it and stays on OpenGL, so it runs without the Metal renderer's
  speedup; mcopt's other optimizations stay on. (0.2.0-alpha.1 crashed with it instead: add `mcopt.metal=false` to
  `config/mcopt.properties` there.)
- Far terrain is experimental. On Macs with fewer than 10 GPU cores, it costs most of the fps.
- With fewer than 10 GPU cores, or 8 GB of memory or less, the profile leaves out the bigger chunk cache.
- With Lithium, C2ME or ScalableLux installed, some of mcopt's chunk patches step aside on purpose.

## Plans

- Far terrain on by default, with tiers for smaller Macs
- Our own shaders (BSL-level) as an option
- Faster world generation (native noise, bit-exact)
- More exact server-tick and chunk optimizations, held for later alphas
- Faster startup
- Wider hardware testing

## Licence and credits

[Apache License 2.0](LICENSE). Keep the [NOTICE](NOTICE) file with any copy, and credit mcopt. Files adapted from
[Sodium](https://github.com/CaffeineMC/sodium) keep the [PolyForm Shield License 1.0.0](LICENSES/PolyForm-Shield-1.0.0.md).
Each of those files says so in its header, and NOTICE lists them. mcopt runs on top of Sodium, by JellySquid and its
contributors.
