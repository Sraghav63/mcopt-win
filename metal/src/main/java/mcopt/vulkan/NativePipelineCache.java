package mcopt.vulkan;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;
import static org.lwjgl.vulkan.VK12.*;

/** One VkPipelineCache per logical device, shared by compilation workers.
 * Read locks allow concurrent compiles; the Vulkan cache uses its default internal synchronization.
 * Shutdown takes the write lock before snapshot/destruction, so no compile can use a destroyed cache.
 */
public final class NativePipelineCache implements AutoCloseable {
	private final VkDevice device;
	private final PipelineCacheStore.Identity identity;
	private final Path file;
	private final ReentrantReadWriteLock lifetime = new ReentrantReadWriteLock();
	private long handle;
	private NativePipelineCache(VkDevice device, PipelineCacheStore.Identity identity, Path file, long handle) {
		this.device = device; this.identity = identity; this.file = file; this.handle = handle;
	}
	public static NativePipelineCache open(VkDevice device, Path directory) {
		try (var stack = MemoryStack.stackPush()) {
			var properties = VkPhysicalDeviceProperties.malloc(stack);
			vkGetPhysicalDeviceProperties(device.getPhysicalDevice(), properties);
			byte[] uuid = new byte[VK_UUID_SIZE]; properties.pipelineCacheUUID().get(uuid);
			var identity = new PipelineCacheStore.Identity(properties.vendorID(), properties.deviceID(), properties.driverVersion(), uuid);
			Path file = directory.resolve(identity.filename());
			byte[] saved = null;
			try { saved = PipelineCacheStore.read(file, identity); }
			catch (java.io.IOException e) { System.err.println("[mcopt] Vulkan cache read skipped: " + e); }
			ByteBuffer initial = saved == null ? null : MemoryUtil.memAlloc(saved.length).put(saved).flip();
			try {
				var create = VkPipelineCacheCreateInfo.calloc(stack).sType$Default().pInitialData(initial);
				var out = stack.callocLong(1);
				int rc = vkCreatePipelineCache(device, create, null, out);
				boolean restored = saved != null && rc == VK_SUCCESS;
				if (rc != VK_SUCCESS && initial != null) {
					create.pInitialData(null);
					rc = vkCreatePipelineCache(device, create, null, out);
				}
				if (rc != VK_SUCCESS) {
					System.err.println("[mcopt] Vulkan pipeline cache unavailable (" + rc + "); normal pipeline compilation retained");
					return null;
				}
				System.out.println("[mcopt] Vulkan pipeline cache: " + (!restored ? "cold" : "loaded " + saved.length + " bytes") + " / " + file.getFileName());
				return new NativePipelineCache(device, identity, file, out.get(0));
			} finally { if (initial != null) MemoryUtil.memFree(initial); }
		}
	}
	public int compile(VkGraphicsPipelineCreateInfo.Buffer infos, VkAllocationCallbacks allocator, LongBuffer pipelines) {
		var lock = lifetime.readLock(); lock.lock();
		try { return vkCreateGraphicsPipelines(device, handle, infos, allocator, pipelines); }
		finally { lock.unlock(); }
	}
	@Override public void close() {
		var lock = lifetime.writeLock(); lock.lock();
		try {
			if (handle == VK_NULL_HANDLE) return;
			try (var stack = MemoryStack.stackPush()) {
				var size = stack.callocPointer(1);
				if (vkGetPipelineCacheData(device, handle, size, (ByteBuffer) null) == VK_SUCCESS && size.get(0) >= 32 && size.get(0) <= PipelineCacheStore.MAX_BYTES) {
					ByteBuffer data = MemoryUtil.memAlloc((int) size.get(0));
					try {
						int rc = vkGetPipelineCacheData(device, handle, size, data);
						if (rc == VK_SUCCESS || rc == VK_INCOMPLETE) {
							byte[] bytes = new byte[(int) size.get(0)]; data.get(bytes);
							PipelineCacheStore.write(file, identity, bytes);
							System.out.println("[mcopt] Vulkan pipeline cache saved: " + bytes.length + " bytes");
						}
					} finally { MemoryUtil.memFree(data); }
				}
			} catch (java.io.IOException | RuntimeException e) {
				System.err.println("[mcopt] Vulkan cache save skipped: " + e);
			} finally { vkDestroyPipelineCache(device, handle, null); handle = VK_NULL_HANDLE; }
		} finally { lock.unlock(); }
	}
}
