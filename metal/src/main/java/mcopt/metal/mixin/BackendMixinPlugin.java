package mcopt.metal.mixin;

import java.util.List;
import java.util.Set;
import mcopt.metal.Platform;
import mcopt.metal.Profile;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Never transform game classes with Apple renderer hooks on Windows. */
public final class BackendMixinPlugin implements IMixinConfigPlugin {
	static { Profile.apply(); }
	@Override public void onLoad(String mixinPackage) { }
	@Override public String getRefMapperConfig() { return null; }
	@Override public boolean shouldApplyMixin(String target, String mixin) { return Platform.metalEnabled(); }
	@Override public void acceptTargets(Set<String> mine, Set<String> others) { }
	@Override public List<String> getMixins() { return null; }
	@Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
	@Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
