package mcopt.vulkan;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class VulkanPortTest {
	private static ByteBuffer commands(boolean indirect, int[][] values) {
		var b = ByteBuffer.allocate(values.length * (indirect ? 20 : 12)).order(ByteOrder.nativeOrder());
		for (var v : values) {
			if (indirect) b.putInt(v[1]).putInt(v[3]).putInt(v[0]).putInt(v[2]).putInt(v[4]);
			else b.putInt(v[0]).putInt(v[1]).putInt(v[2]);
		}
		return b.flip();
	}
	private static List<Long> triangleVertices(ByteBuffer b, int draws, boolean indirect) {
		var result = new ArrayList<Long>();
		int stride = indirect ? 20 : 12;
		int[] pattern = {0, 1, 2, 2, 3, 0};
		for (int d = 0; d < draws; d++) {
			int p = d * stride, first = b.getInt(p + (indirect ? 8 : 0)), count = b.getInt(p + (indirect ? 0 : 4));
			int base = b.getInt(p + (indirect ? 12 : 8)), instances = indirect ? b.getInt(p + 4) : 1;
			int firstInstance = indirect ? b.getInt(p + 16) : 0;
			for (int instance = 0; instance < instances; instance++) for (int i = first; i < first + count; i++)
				result.add(((long) (firstInstance + instance) << 32) | Integer.toUnsignedLong(base + i / 6 * 4 + pattern[i % 6]));
		}
		return result;
	}
	@Test public void adjacentQuadRangesMergeWithoutChangingTriangles() {
		for (boolean indirect : new boolean[] {false, true}) {
			var input = commands(indirect, new int[][] {{0, 6, 100, 1, 0}, {0, 12, 104, 1, 0}, {0, 6, 300, 1, 0}});
			byte[] original = input.array().clone();
			var output = ByteBuffer.allocate(input.capacity()).order(ByteOrder.nativeOrder());
			int merged = TerrainDrawMerge.merge(input, 3, indirect, output);
			assertEquals(2, merged);
			assertEquals(triangleVertices(input, 3, indirect), triangleVertices(output, merged, indirect));
			assertArrayEquals(original, input.array());
		}
	}
	@Test public void randomizedBatchesKeepEveryVertexAndInstanceInOrder() {
		var random = new Random(91822);
		for (boolean indirect : new boolean[] {false, true}) for (int run = 0; run < 500; run++) {
			int[][] values = new int[1 + random.nextInt(30)][5];
			int base = 0;
			for (var v : values) {
				v[0] = random.nextInt(6) == 0 ? 6 : 0;
				v[1] = random.nextInt(12) * 6;
				v[2] = base;
				v[3] = random.nextInt(5) == 0 ? 2 : 1;
				v[4] = random.nextInt(5) == 0 ? 3 : 0;
				base += v[1] / 6 * 4 + (random.nextBoolean() ? 0 : 4);
			}
			var input = commands(indirect, values);
			var output = ByteBuffer.allocate(input.capacity()).order(ByteOrder.nativeOrder());
			int merged = TerrainDrawMerge.merge(input, values.length, indirect, output);
			assertEquals(triangleVertices(input, values.length, indirect), triangleVertices(output, merged, indirect));
		}
	}
	@Test public void sharedIndexCapacityAndSignedVertexOverflowPreventMerging() {
		for (boolean indirect : new boolean[] {false, true}) {
			var in = commands(indirect, new int[][] {{0, 98304, 0, 1, 0}, {0, 6, 65536, 1, 0}});
			var out = ByteBuffer.allocate(in.capacity()).order(ByteOrder.nativeOrder());
			assertEquals(2, TerrainDrawMerge.merge(in, 2, indirect, out));
			in = commands(indirect, new int[][] {{0, 6, Integer.MAX_VALUE - 1, 1, 0}, {0, 6, Integer.MIN_VALUE + 2, 1, 0}});
			assertEquals(2, TerrainDrawMerge.merge(in, 2, indirect, out));
		}
	}
	@Test public void bindingCacheTracksOffsetsTypesSlotsAndPassLifetime() {
		var s = new BindingState();
		assertTrue(s.pipeline(10)); assertFalse(s.pipeline(10)); assertTrue(s.pipeline(11)); assertTrue(s.pipeline(10));
		assertTrue(s.index(50, 0, 1)); assertFalse(s.index(50, 0, 1)); assertTrue(s.index(50, 0, 2)); assertTrue(s.index(50, 16, 2));
		assertTrue(s.vertex(0, 60, 0)); assertFalse(s.vertex(0, 60, 0)); assertTrue(s.vertex(1, 60, 0)); assertTrue(s.vertex(0, 60, 4));
		s.invalidateVertices(0, 2); assertTrue(s.vertex(0, 60, 4)); assertTrue(s.vertex(1, 60, 0));
		assertTrue(s.vertex(100, 60, 0)); assertTrue(s.vertex(100, 60, 0));
		assertTrue(new BindingState().pipeline(10));
	}
	private static byte[] cacheData(PipelineCacheStore.Identity id) {
		return ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN).putInt(32).putInt(1).putInt(id.vendor()).putInt(id.device()).put(id.uuid()).putLong(1234).array();
	}
	@Test public void cacheRejectsCorruptionTruncationAndAnotherGpu() throws Exception {
		var identity = new PipelineCacheStore.Identity(1, 2, 3, new byte[16]);
		Path dir = Files.createTempDirectory("mcopt-cache-"); Path file = dir.resolve(identity.filename());
		try {
			byte[] data = cacheData(identity);
			PipelineCacheStore.write(file, identity, data);
			assertArrayEquals(data, PipelineCacheStore.read(file, identity));
			assertNull(PipelineCacheStore.read(file, new PipelineCacheStore.Identity(1, 5, 3, new byte[16])));
			byte[] damaged = Files.readAllBytes(file); damaged[damaged.length - 1] ^= 1; Files.write(file, damaged);
			assertNull(PipelineCacheStore.read(file, identity));
			Files.write(file, new byte[12]); assertNull(PipelineCacheStore.read(file, identity));
			assertNotEquals(identity.filename(), new PipelineCacheStore.Identity(1, 2, 4, new byte[16]).filename());
			assertFalse(PipelineCacheStore.compatible(new byte[31], identity));
		} finally { Files.deleteIfExists(file); Files.deleteIfExists(dir); }
	}
	@Test public void concurrentCacheWritesPublishWholeFilesAndLargeFilesAreSkipped() throws Exception {
		var id = new PipelineCacheStore.Identity(1, 2, 3, new byte[16]);
		Path dir = Files.createTempDirectory("mcopt-cache-writers-"); Path file = dir.resolve(id.filename());
		try (var workers = java.util.concurrent.Executors.newFixedThreadPool(4)) {
			var futures = new ArrayList<java.util.concurrent.Future<?>>();
			for (int i = 0; i < 12; i++) {
				final int payload = i;
				futures.add(workers.submit(() -> {
					byte[] bytes = cacheData(id); bytes[32] = (byte) payload;
					try { PipelineCacheStore.write(file, id, bytes); }
					catch (java.io.IOException e) { throw new IllegalStateException(e); }
				}));
			}
			for (var future : futures) future.get();
			assertNotNull(PipelineCacheStore.read(file, id));
			try (var large = new java.io.RandomAccessFile(file.toFile(), "rw")) { large.setLength(PipelineCacheStore.MAX_BYTES + 17L); }
			assertNull(PipelineCacheStore.read(file, id));
		} finally {
			try (var files = Files.list(dir)) { for (Path path : files.toList()) Files.delete(path); }
			Files.delete(dir);
		}
	}

	@Test public void preferredBackendRetainsFallbackOrderWithoutDuplicates() {
		assertEquals(List.of("Vulkan", "OpenGL", "Other"), BackendOrder.prefer(List.of("OpenGL", "Vulkan", "Other"), "Vulkan"::equals));
		assertEquals(List.of("OpenGL", "Other"), BackendOrder.prefer(List.of("OpenGL", "Other"), "Vulkan"::equals));
		assertEquals(List.of("Vulkan", "OpenGL"), BackendOrder.prefer(List.of("Vulkan", "OpenGL"), "Vulkan"::equals));
	}
	@Test public void graphicsPolicyHonorsOpenGlChoiceAndCompatibilityMods() {
		String original = System.getProperty("mcopt.graphics");
		try {
			System.clearProperty("mcopt.graphics");
			assertEquals(VulkanOptions.Choice.VULKAN, VulkanOptions.choose("default", false));
			assertEquals(VulkanOptions.Choice.OPENGL, VulkanOptions.choose("opengl", false));
			assertEquals(VulkanOptions.Choice.OPENGL, VulkanOptions.choose("default", true));
			System.setProperty("mcopt.graphics", "vulkan"); assertEquals(VulkanOptions.Choice.VULKAN, VulkanOptions.choose("opengl", false));
			System.setProperty("mcopt.graphics", "vanilla"); assertEquals(VulkanOptions.Choice.VANILLA, VulkanOptions.choose("default", false));
		} finally { if (original == null) System.clearProperty("mcopt.graphics"); else System.setProperty("mcopt.graphics", original); }
	}
}
