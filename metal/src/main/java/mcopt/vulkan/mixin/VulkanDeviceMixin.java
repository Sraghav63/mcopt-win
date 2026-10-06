package mcopt.vulkan.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import mcopt.vulkan.NativePipelineCache;
import mcopt.vulkan.VulkanCacheAccess;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VulkanDevice.class, remap = false)
abstract class VulkanDeviceMixin implements VulkanCacheAccess {
	@Unique private NativePipelineCache mcopt$cache;
	@Inject(method = "<init>", at = @At("RETURN"))
	private void mcopt$openCache(CallbackInfo ci) {
		try {
			mcopt$cache = NativePipelineCache.open(((VulkanDevice) (Object) this).vkDevice(),
				FabricLoader.getInstance().getGameDir().resolve("cache/mcopt/vulkan"));
		} catch (RuntimeException e) { System.err.println("[mcopt] Vulkan pipeline cache disabled: " + e); }
	}
	@Inject(method = "close", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkDestroyDevice(Lorg/lwjgl/vulkan/VkDevice;Lorg/lwjgl/vulkan/VkAllocationCallbacks;)V"))
	private void mcopt$closeCache(CallbackInfo ci) {
		if (mcopt$cache != null) { mcopt$cache.close(); mcopt$cache = null; }
	}
	@Override public NativePipelineCache mcopt$pipelineCache() { return mcopt$cache; }
}
