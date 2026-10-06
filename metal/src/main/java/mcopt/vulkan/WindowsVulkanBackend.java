package mcopt.vulkan;

import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanBackend;

/** Reuses Minecraft's native Vulkan device, feature negotiation, shaders, surfaces and queues.
 * The mcopt Vulkan mixins optimize pipeline compilation, terrain batches and command recording.
 */
public final class WindowsVulkanBackend extends VulkanBackend {
	@Override public GpuDevice createDevice(GpuDebugOptions options) throws BackendCreationException {
		GpuDevice device = super.createDevice(options);
		var info = device.getDeviceInfo();
		System.out.println("[mcopt] Vulkan GPU path active: " + info.name() + " / " + info.driverInfo()
			+ "; pipeline cache=" + VulkanOptions.flag("pipelineCache")
			+ ", state cache=" + VulkanOptions.flag("stateCache")
			+ ", terrain merging=" + VulkanOptions.flag("mergeDraws"));
		return device;
	}
}
