package mcopt.metal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class WindowsPortTest {
	private Properties original;
	private Path config;
	@Before public void setup() throws Exception {
		original = (Properties) System.getProperties().clone();
		System.getProperties().keySet().removeIf(k -> k.toString().startsWith("mcopt."));
		System.setProperty("os.name", "Windows 11");
		System.setProperty("os.arch", "amd64");
		config = Files.createTempDirectory("mcopt-config-").resolve("mcopt.properties");
	}
	@After public void restore() throws Exception {
		System.setProperties(original);
		Files.deleteIfExists(config);
		Files.deleteIfExists(config.getParent());
	}
	@Test public void windowsDefaultEnablesPortableOptimizations() throws Exception {
		Profile.applyFlags(config);
		Profile.platformFlags();
		assertTrue(Files.readString(config).contains("profile=windows"));
		for (String key : new String[] {"mcopt.cpu.model", "mcopt.cpu.lists", "mcopt.cpu.cullRecover", "mcopt.alloc.lightMap", "mcopt.chunk.saveSkip"})
			assertEquals(key, "true", System.getProperty(key));
		assertEquals("all,biome", System.getProperty("mcopt.chunk.mesh"));
		assertEquals("lazy", System.getProperty("mcopt.startup.narrator"));
		assertFalse(Platform.metalEnabled());
	}
	@Test public void copiedMacFlagsCannotEnableAppleHooks() throws Exception {
		Files.writeString(config, "profile=recommended\nmcopt.metal=true\nmcopt.lod=true\nmcopt.preciseLimiter=true\n");
		System.setProperty("mcopt.metal", "true");
		System.setProperty("mcopt.lod.radius", "256");
		System.setProperty("mcopt.gen.nativeNoise", "neon");
		System.setProperty("mcopt.gen.worldgenQos", "interactive");
		System.setProperty("mcopt.metal.latency", "true");
		Profile.applyFlags(config);
		Profile.platformFlags();
		for (String key : new String[] {"mcopt.metal", "mcopt.lod", "mcopt.preciseLimiter", "mcopt.cpu.texelCache"})
			assertEquals(key, "false", System.getProperty(key));
		System.setProperty("mcopt.profile", "none"); // plugin initialization stays isolated
		assertFalse(new mcopt.metal.mixin.BackendMixinPlugin().shouldApplyMixin("unused", "PreferredGraphicsApiMixin"));
		assertFalse(new mcopt.metal.mixin.lod.LodMixinPlugin().shouldApplyMixin("unused", "LodLevelRendererMixin"));
		assertFalse(new mcopt.metal.mixin.gen.GenMixinPlugin().shouldApplyMixin("unused", ".GenNoiseChunkMixin"));
		assertFalse(new mcopt.metal.mixin.gen.GenMixinPlugin().shouldApplyMixin("unused", ".GenWorldgenQosMixin"));
		assertFalse(new mcopt.metal.mixin.probe.ProbeMixinPlugin().shouldApplyMixin("unused", "PollProbeMixin"));
		TexelViewsSwitch.pass(true); // must not extract or load the Apple library
	}
	@Test public void commandLineWinsOverConfigAndConfigWinsOverProfile() throws Exception {
		Files.writeString(config, "profile=windows\nmcopt.cpu.model=false\nmcopt.cpu.lists=false\n");
		System.setProperty("mcopt.cpu.lists", "true");
		Profile.applyFlags(config);
		assertEquals("false", System.getProperty("mcopt.cpu.model"));
		assertEquals("true", System.getProperty("mcopt.cpu.lists"));
	}
	@Test public void noneDoesNotEnablePortableProfile() throws Exception {
		Files.writeString(config, "profile=none\n");
		Profile.applyFlags(config);
		Profile.platformFlags();
		assertNull(System.getProperty("mcopt.chunk.mesh"));
		assertNull(System.getProperty("mcopt.cpu.model"));
	}
	@Test public void windowsPlatformAccepts64BitJavaAndRejects32Bit() throws Exception {
		var problem = PlatformCheck.class.getDeclaredMethod("problem", String.class, String.class, String.class);
		problem.setAccessible(true);
		for (String arch : new String[] {"amd64", "x86_64", "aarch64", "arm64"}) {
			assertNull(problem.invoke(null, "Windows 11", arch, "10.0"));
			assertNull(problem.invoke(null, "Windows 10", arch, "10.0"));
		}
		assertNotNull(problem.invoke(null, "Windows 11", "x86", "10.0"));
		assertNotNull(problem.invoke(null, "Linux", "amd64", "6.1"));
	}
	@Test public void nativeWindowsSchedulingSetsCurrentThreadPriority() throws Throwable {
		org.junit.Assume.assumeTrue(original.getProperty("os.name", "").startsWith("Windows"));
		var lib = java.lang.foreign.SymbolLookup.libraryLookup("Kernel32.dll", java.lang.foreign.Arena.global());
		var linker = java.lang.foreign.Linker.nativeLinker();
		var current = linker.downcallHandle(lib.find("GetCurrentThread").orElseThrow(),
			java.lang.foreign.FunctionDescriptor.of(java.lang.foreign.ValueLayout.ADDRESS));
		var get = linker.downcallHandle(lib.find("GetThreadPriority").orElseThrow(),
			java.lang.foreign.FunctionDescriptor.of(java.lang.foreign.ValueLayout.JAVA_INT, java.lang.foreign.ValueLayout.ADDRESS));
		var set = linker.downcallHandle(lib.find("SetThreadPriority").orElseThrow(),
			java.lang.foreign.FunctionDescriptor.of(java.lang.foreign.ValueLayout.JAVA_INT, java.lang.foreign.ValueLayout.ADDRESS, java.lang.foreign.ValueLayout.JAVA_INT));
		java.lang.foreign.MemorySegment thread = (java.lang.foreign.MemorySegment) current.invokeExact();
		int previous = (int) get.invokeExact(thread);
		try {
			WindowsQos.self("render", 0x21);
			assertEquals(1, (int) get.invokeExact(thread));
		} finally {
			int restored = (int) set.invokeExact(thread, previous);
			assertNotEquals(0, restored);
		}
	}

	@Test public void schedulerMappingsStayBelowRealtime() {
		assertEquals(-2, WindowsQos.priority(0x09));
		assertEquals(-1, WindowsQos.priority(0x11));
		assertEquals(0, WindowsQos.priority(0x15));
		assertEquals(1, WindowsQos.priority(0x19));
		assertEquals(1, WindowsQos.priority(0x21));
	}
}
