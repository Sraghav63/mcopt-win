package mcopt.vulkan.mixin;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import mcopt.vulkan.TerrainDrawMerge;
import mcopt.vulkan.VulkanStats;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.VKIndirectDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import net.caffeinemc.mods.sodium.client.gpu.device.context.VKIndirectContext;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VKIndirectDrawBatch.class, remap = false)
abstract class VulkanIndirectBatchMixin extends MultiDrawBatch {
	@Shadow @Final private long pCommands;
	@Shadow private long offset;
	@Unique private ByteBuffer mcopt$scratch;
	@Unique private int mcopt$merged = -1;
	@Inject(method = "prepare", at = @At("HEAD"), cancellable = true)
	private void mcopt$merge(DrawContext context, CallbackInfo ci) {
		mcopt$merged = -1;
		if (size < 2) return;
		int bytes = Math.multiplyExact(size, 20);
		if (mcopt$scratch == null || mcopt$scratch.capacity() < bytes) {
			if (mcopt$scratch != null) MemoryUtil.memFree(mcopt$scratch);
			mcopt$scratch = MemoryUtil.memAlloc(bytes).order(ByteOrder.nativeOrder());
		}
		int merged = TerrainDrawMerge.merge(MemoryUtil.memByteBuffer(pCommands, bytes), size, true, mcopt$scratch);
		VulkanStats.terrain(size, merged);
		if (merged == size) return;
		var vk = (VKIndirectContext) context;
		offset = vk.addCommand(merged * 20);
		MemoryUtil.memCopy(MemoryUtil.memAddress(mcopt$scratch), MemoryUtil.memAddress(vk.mappedView.data()) + offset, merged * 20L);
		mcopt$merged = merged;
		ci.cancel();
	}
	@Inject(method = "draw", at = @At("HEAD"), cancellable = true)
	private void mcopt$draw(DrawContext context, CallbackInfo ci) {
		if (mcopt$merged < 0) return;
		var vk = (VKIndirectContext) context;
		context.getPass().drawIndexedIndirect(vk.mappedView.slice().slice(offset, mcopt$merged * 20L), mcopt$merged);
		ci.cancel();
	}
	@Inject(method = "delete", at = @At("HEAD"))
	private void mcopt$free(CallbackInfo ci) {
		if (mcopt$scratch != null) { MemoryUtil.memFree(mcopt$scratch); mcopt$scratch = null; }
	}
}
