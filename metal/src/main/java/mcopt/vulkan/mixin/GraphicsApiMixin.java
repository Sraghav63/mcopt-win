package mcopt.vulkan.mixin;

import java.util.ArrayList;
import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.backend.opengl.GlBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanBackend;
import mcopt.vulkan.VulkanOptions;
import mcopt.vulkan.BackendOrder;
import mcopt.vulkan.WindowsVulkanBackend;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.PreferredGraphicsApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PreferredGraphicsApi.class)
abstract class GraphicsApiMixin {
	@Inject(method = "getBackendsToTry", at = @At("RETURN"), cancellable = true)
	private void mcopt$vulkanFirst(CallbackInfoReturnable<GpuBackend[]> cir) {
		var loader = FabricLoader.getInstance();
		boolean openGlMod = loader.isModLoaded("distanthorizons") || loader.isModLoaded("iris");
		var choice = VulkanOptions.choose(((PreferredGraphicsApi) (Object) this).getSerializedName(), openGlMod);
		if (choice == VulkanOptions.Choice.VANILLA) return;
		var backends = new ArrayList<GpuBackend>();
		boolean compatibilityOnly = openGlMod && choice == VulkanOptions.Choice.OPENGL;

		for (GpuBackend backend : cir.getReturnValue()) {
			if (compatibilityOnly && backend instanceof VulkanBackend) continue;
			if (backend instanceof VulkanBackend) backend = new WindowsVulkanBackend();
			backends.add(backend);
		}
		var ordered = BackendOrder.prefer(backends, backend -> choice == VulkanOptions.Choice.VULKAN ? backend instanceof VulkanBackend : backend instanceof GlBackend);
		System.out.println("[mcopt] graphics preference=" + choice + (compatibilityOnly ? "; Vulkan omitted for OpenGL mod compatibility" : "; initialization failures retain Minecraft's backend fallback")
			+ (openGlMod ? "; OpenGL-dependent mod detected" : ""));
		cir.setReturnValue(ordered.toArray(GpuBackend[]::new));
	}
}
