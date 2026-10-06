# Windows Vulkan integration verification

The Windows GPU path builds on Minecraft 26.3's native Vulkan backend. mcopt chooses the backend and adds persistent
pipeline caching, pass-local binding suppression, and contiguous Sodium terrain-range merging. This is not a port of
Metal's entire terrain renderer: split-pass GPU occlusion, MetalFX, generated frames and mcopt far terrain remain absent.

## Verified locally

- Java 25 / Gradle Windows-target compilation and single-jar/zip packaging.
- Automated tests cover config precedence, Windows/Apple feature gating, backend policy and fallback ordering,
  binding identity/offset/type/pass scope, malformed and concurrent cache files, index capacity and integer overflow.
- 1,000 randomized direct/indirect batches preserve the full triangle vertex and instance sequence after merging.
- Hook contract tests inspect the actual Minecraft and Sodium class files, including every native invocation descriptor.
- A development-only Fabric fixture opens an isolated demo world, renders for 30 seconds, captures the game's output,
  then shuts down cleanly. It is excluded from release jars.
- Two real GPU client runs on an Apple M4 through MoltenVK 1.4.2 successfully exercised the Vulkan pipeline/device,
  render-pass and Sodium indirect-batch hooks. The direct multi-draw class was also transformed successfully, although
  this GPU lacks EXT_multi_draw, so that draw path was not executed on hardware.
- The first client run recorded 249,878 redundant native bindings skipped out of 414,945 binding requests; terrain ranges
  reduced from 1,028,565 to 1,025,231. These counters are cumulative, not frame rates or GPU-time measurements.
- Clean exit saved 1,260,713 bytes of pipeline data. The second launch loaded that data and saved it again on exit.
- Visual inspection of the captured world found no obvious terrain corruption. This is not a pixel-equivalence test.

The Mac Vulkan driver logged warnings about unsupported primitive-restart disabling in the existing Minecraft pipeline
setup. This run does not establish error-free Vulkan validation or Windows driver behavior. Offline Fabric demo mode
also produces expected Realms authentication warnings; no real account is used.

## Still requires Windows hardware

Real Windows startup, rendering, resize/fullscreen/presentation, device/driver failure handling, reloads, long sessions,
and measured FPS/frame times on NVIDIA, AMD and Intel. The Windows thread API test runs on Windows CI; native Vulkan
rendering requires a suitable GPU/driver and is not exercised by the ordinary hosted CI build. No Windows performance
parity claim is made.

## Reproduce

Build and tests:

```sh
./gradlew -PtargetPlatform=windows :metal:build :fpshud:build :metal:windowsRelease
```

On Windows use `gradlew.bat` instead. For an actual isolated client smoke test:

```sh
./gradlew -I tools/vulkan-smoke.init.gradle -PtargetPlatform=windows :metal:runClient
```

This downloads game assets, opens a Minecraft window, and creates only `metal/build/vulkan-smoke-game` for game data.
It uses offline demo mode and copies the build-time Sodium dependency into that isolated game's mods folder. It is a
functional smoke test, not a timed benchmark. It captures `screenshots/mcopt-vulkan-smoke.png`, writes
`smoke-complete.txt`, and exits. Run twice to check cache save/load. The `testOtherPlatforms` switch is enabled by this
fixture so the same Vulkan hooks can be verified through MoltenVK on a Mac; normal Mac launches retain the Metal path.

For performance measurements use the controlled same-backend procedure in WINDOWS-README.txt. State caching and
range merging can also be checked with `mcopt.vulkan.stats=true`; counters alone do not establish an FPS improvement.
