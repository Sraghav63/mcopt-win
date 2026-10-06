package mcopt.vulkan.mixin;

import java.nio.LongBuffer;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import mcopt.vulkan.BindingState;
import mcopt.vulkan.VulkanStats;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import static org.lwjgl.vulkan.VK12.*;

/** Reduce native call/command-stream overhead. The game's uniforms, layout changes and draws still run normally. */
@Mixin(value = VulkanRenderPass.class, remap = false)
abstract class VulkanRenderPassMixin {
	@Unique private final BindingState mcopt$bindings = new BindingState();
	@Redirect(method = "setPipeline", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkCmdBindPipeline(Lorg/lwjgl/vulkan/VkCommandBuffer;IJ)V"))
	private void mcopt$bindPipeline(VkCommandBuffer command, int point, long pipeline) {
		boolean issued = mcopt$bindings.pipeline(pipeline); VulkanStats.binding(issued);
		if (issued) vkCmdBindPipeline(command, point, pipeline);
	}
	@Redirect(method = "setIndexBuffer", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkCmdBindIndexBuffer(Lorg/lwjgl/vulkan/VkCommandBuffer;JJI)V"))
	private void mcopt$bindIndex(VkCommandBuffer command, long buffer, long offset, int type) {
		boolean issued = mcopt$bindings.index(buffer, offset, type); VulkanStats.binding(issued);
		if (issued) vkCmdBindIndexBuffer(command, buffer, offset, type);
	}
	@Redirect(method = "setVertexBuffer", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkCmdBindVertexBuffers(Lorg/lwjgl/vulkan/VkCommandBuffer;ILjava/nio/LongBuffer;Ljava/nio/LongBuffer;)V"))
	private void mcopt$bindVertex(VkCommandBuffer command, int slot, LongBuffer buffers, LongBuffer offsets) {
		if (buffers.remaining() != 1 || offsets.remaining() != 1) {
			mcopt$bindings.invalidateVertices(slot, buffers.remaining());
			vkCmdBindVertexBuffers(command, slot, buffers, offsets);
		} else {
			boolean issued = mcopt$bindings.vertex(slot, buffers.get(buffers.position()), offsets.get(offsets.position()));
			VulkanStats.binding(issued);
			if (issued) vkCmdBindVertexBuffers(command, slot, buffers, offsets);
		}
	}
}
