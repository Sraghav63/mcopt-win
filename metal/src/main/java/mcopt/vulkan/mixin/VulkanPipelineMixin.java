package mcopt.vulkan.mixin;

import java.nio.LongBuffer;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import mcopt.vulkan.VulkanCacheAccess;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import static org.lwjgl.vulkan.VK12.vkCreateGraphicsPipelines;

@Mixin(value = VulkanRenderPipeline.class, remap = false)
abstract class VulkanPipelineMixin {
	@Redirect(method = "compile", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkCreateGraphicsPipelines(Lorg/lwjgl/vulkan/VkDevice;JLorg/lwjgl/vulkan/VkGraphicsPipelineCreateInfo$Buffer;Lorg/lwjgl/vulkan/VkAllocationCallbacks;Ljava/nio/LongBuffer;)I"), require = 2)
	private static int mcopt$cachedPipelines(VkDevice vk, long originalCache, VkGraphicsPipelineCreateInfo.Buffer infos,
		VkAllocationCallbacks allocator, LongBuffer pipelines, VulkanDevice owner, BackendRenderPipeline.CreateInfo info) {
		var cache = ((VulkanCacheAccess) owner).mcopt$pipelineCache();
		return cache == null ? vkCreateGraphicsPipelines(vk, originalCache, infos, allocator, pipelines) : cache.compile(infos, allocator, pipelines);
	}
}
