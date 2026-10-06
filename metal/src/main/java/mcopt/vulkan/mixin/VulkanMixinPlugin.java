package mcopt.vulkan.mixin;

import java.util.List;
import java.util.Set;
import mcopt.metal.Profile;
import mcopt.vulkan.VulkanOptions;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class VulkanMixinPlugin implements IMixinConfigPlugin {
	static { Profile.apply(); }
	@Override public void onLoad(String mixinPackage) { }
	@Override public String getRefMapperConfig() { return null; }
	@Override public boolean shouldApplyMixin(String target, String mixin) {
		if (!VulkanOptions.platformEnabled()) return false;
		if (mixin.endsWith("GraphicsApiMixin")) return true;
		if (mixin.endsWith("VulkanDeviceMixin") || mixin.endsWith("VulkanPipelineMixin")) return VulkanOptions.flag("pipelineCache");
		if (mixin.endsWith("VulkanRenderPassMixin")) return VulkanOptions.flag("stateCache");
		return VulkanOptions.flag("mergeDraws");
	}
	@Override public void acceptTargets(Set<String> mine, Set<String> others) { }
	@Override public List<String> getMixins() { return null; }
	@Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
	@Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
