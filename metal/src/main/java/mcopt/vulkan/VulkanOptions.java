package mcopt.vulkan;

import java.util.Locale;
import mcopt.metal.Platform;

/** Safe to read during Mixin selection: no Minecraft or native classes. */
public final class VulkanOptions {
	private VulkanOptions() { }
	public static boolean platformEnabled() {
		return Platform.windows() || Boolean.getBoolean("mcopt.vulkan.testOtherPlatforms");
	}
	public static boolean flag(String name) {
		return platformEnabled() && Boolean.getBoolean("mcopt.vulkan." + name);
	}
	public enum Choice { VULKAN, OPENGL, VANILLA }
	public static Choice choose(String gamePreference, boolean openGlMod) {
		String mode = System.getProperty("mcopt.graphics", "auto").trim().toLowerCase(Locale.ROOT);
		return switch (mode) {
			case "vanilla" -> Choice.VANILLA;
			case "opengl" -> Choice.OPENGL;
			case "vulkan" -> Choice.VULKAN;
			default -> openGlMod || "opengl".equals(gamePreference) ? Choice.OPENGL : Choice.VULKAN;
		};
	}
}
