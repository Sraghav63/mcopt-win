package mcopt.smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import mcopt.vulkan.VulkanStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.InputWithModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Development-only fixture, packaged separately and NEVER included in the release jar. */
@Mixin(Minecraft.class)
abstract class ClientSmokeMixin {
	@Unique private boolean mcopt$started, mcopt$captured;
	@Unique private volatile boolean mcopt$callback;
	@Unique private long mcopt$worldStarted, mcopt$capturedAt;
	@Inject(method = "runTick", at = @At("RETURN"))
	private void mcopt$smoke(boolean tick, CallbackInfo ci) {
		if (!Boolean.getBoolean("mcopt.smoke")) return;
		Minecraft mc = (Minecraft) (Object) this;
		if (mc.gui.overlay() != null) return;
		if (mc.level == null && !mcopt$started) {
			if (!(mc.gui.screen() instanceof TitleScreen)) { mc.gui.setScreen(new TitleScreen()); return; }
			for (var child : mc.gui.screen().children()) if (child instanceof Button b && b.getMessage().getString().startsWith("Play Demo")) {
				mcopt$started = true;
				b.onPress(new InputWithModifiers() { public int input() { return 0; } public int modifiers() { return 0; } });
				System.out.println("[mcopt-smoke] Opening isolated demo world");
				return;
			}
		}
		if (mc.level == null || mc.player == null) return;
		mc.options.pauseOnLostFocus = false;
		mc.gui.setScreen(null);
		if (mcopt$worldStarted == 0) {
			mcopt$worldStarted = System.nanoTime();
			try { Class.forName("net.caffeinemc.mods.sodium.client.gpu.device.batch.VKMultiDrawBatch"); }
			catch (ClassNotFoundException e) { throw new IllegalStateException(e); }
			mc.options.renderDistance().set(8);
		}
		if (!mcopt$captured && System.nanoTime() - mcopt$worldStarted > 30_000_000_000L) {
			mcopt$captured = true; mcopt$capturedAt = System.nanoTime();
			System.out.println("[mcopt-smoke] " + VulkanStats.summary());
			Screenshot.grab(mc.gameDirectory, System.getProperty("mcopt.smoke.filename", "mcopt-vulkan-smoke.png"), mc.gameRenderer.mainRenderTarget(), 1, message -> {
				System.out.println("[mcopt-smoke] Screenshot: " + message.getString());
				mcopt$callback = true;
			});
		}
		if (mcopt$captured && mcopt$callback && System.nanoTime() - mcopt$capturedAt > 3_000_000_000L) {
			try { Files.writeString(Path.of(mc.gameDirectory.toString(), "smoke-complete.txt"), VulkanStats.summary()); }
			catch (java.io.IOException e) { throw new IllegalStateException(e); }
			mc.stop();
		}
	}
}
