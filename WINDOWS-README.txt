mcopt Windows @VERSION@

Windows 11 port (alpha). Back up your worlds before testing.

Requires 64-bit Java 25+, Minecraft Java 26.3, Fabric Loader 0.19.5+
and Sodium 0.9.3 for Minecraft 26.3 (download separately).
https://cdn.modrinth.com/data/AANobbMI/versions/v4PSXean/sodium-fabric-0.9.3-alpha.1%2Bmc26.3.jar

Install
1. Install Fabric for Minecraft 26.3: https://fabricmc.net/use/installer/
2. Press Win+R, enter %APPDATA%\.minecraft\mods and press Enter.
   Create the mods folder if needed. For Prism, use your instance's mods folder.
3. Copy mcopt-windows-@VERSION@.jar and the Sodium jar there.
   Remove older mcopt jars first; the Windows port retains the mcopt-metal mod ID.
4. Start Minecraft with the Fabric profile, using 64-bit Java 25 or newer.

The fps counter is included. Do not install a separate mcopt-fps jar alongside it.
Double-clicking the jar only displays installation help.

The first launch writes config/mcopt.properties with profile=windows.
Portable chunk meshing, render-list, model, light-map, region-write and lazy
narrator optimizations are enabled. The 4096-entry chunk cache needs at least
16 GB physical memory and a 2 GB Java heap; otherwise it is left off.
Explicit chunk-cache overrides in config or JVM arguments take precedence.

Thread scheduling uses Windows SetThreadPriority (render and integrated server
above normal, other roles unchanged). mcopt.qos=off disables it. If Java blocks
native access, add --enable-native-access=ALL-UNNAMED to the launcher's JVM
arguments. A scheduling failure logs once and keeps default Windows priorities.

Apple Metal, MetalFX, frame generation, native noise and mcopt's far terrain
are unavailable. Minecraft/Sodium retains its own graphics backend. Copied Mac
settings cannot activate the Apple native hooks on Windows. No Windows Vulkan
or Direct3D renderer has been added. Mac benchmark numbers do not apply here;
Windows performance parity is not established.

To compare portable optimizations: keep the same world, camera route, resolution,
render/simulation distance, heap, Sodium version and graphics backend. Disable
VSync and the fps cap for both runs. Warm up, then run each route at least three
times with profile=none and profile=windows, restarting Minecraft after each
config change. Keep mcopt.qos=off and mcopt.preload=false in BOTH runs to isolate
profile changes. Record average fps, 1% lows, frame times and server tick times.
Then test QoS and preload separately. profile=none alone does not disable those.

Build on Windows (PowerShell, JDK 25 on PATH or JAVA_HOME):
  .\gradlew.bat -PtargetPlatform=windows :metal:build :fpshud:build :metal:windowsRelease
Outputs: dist/mcopt-windows-@VERSION@.jar and .zip.
Cross-build on Mac/Linux with ./gradlew and the same arguments.

License: Apache-2.0 (LICENSE, NOTICE). Sodium-derived files retain PolyForm Shield;
LICENSES contains its text. Sodium is not bundled.
