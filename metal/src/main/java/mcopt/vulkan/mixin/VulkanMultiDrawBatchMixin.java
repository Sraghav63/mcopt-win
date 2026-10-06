package mcopt.vulkan.mixin;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import mcopt.vulkan.TerrainDrawMerge;
import mcopt.vulkan.VulkanStats;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.VKMultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VKMultiDrawBatch.class, remap = false)
abstract class VulkanMultiDrawBatchMixin extends MultiDrawBatch {
	@Shadow @Final private long pCommands;
	@Unique private ByteBuffer mcopt$scratch;
	@Inject(method = "draw", at = @At("HEAD"), cancellable = true)
	private void mcopt$merge(DrawContext context, CallbackInfo ci) {
		if (size < 2) return;
		int bytes = Math.multiplyExact(size, 12);
		if (mcopt$scratch == null || mcopt$scratch.capacity() < bytes) {
			if (mcopt$scratch != null) MemoryUtil.memFree(mcopt$scratch);
			mcopt$scratch = MemoryUtil.memAlloc(bytes).order(ByteOrder.nativeOrder());
		}
		int merged = TerrainDrawMerge.merge(MemoryUtil.memByteBuffer(pCommands, bytes), size, false, mcopt$scratch);
		VulkanStats.terrain(size, merged);
		if (merged == size) return;
		context.getPass().multiDrawIndexed(MemoryUtil.memIntBuffer(MemoryUtil.memAddress(mcopt$scratch), merged * 3), 1, 0, merged);
		ci.cancel();
	}
	@Inject(method = "delete", at = @At("HEAD"))
	private void mcopt$free(CallbackInfo ci) {
		if (mcopt$scratch != null) { MemoryUtil.memFree(mcopt$scratch); mcopt$scratch = null; }
	}
}
