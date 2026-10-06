package mcopt.vulkan;

/** Opt-in counters on the render thread, for smoke tests and controlled benchmarks. */
public final class VulkanStats {
	private static final boolean ON = Boolean.getBoolean("mcopt.vulkan.stats");
	private static long binds, skipped, ranges, mergedRanges;
	private VulkanStats() { }
	public static void binding(boolean issued) { if (ON) { binds++; if (!issued) skipped++; } }
	public static void terrain(int before, int after) { if (ON) { ranges += before; mergedRanges += after; } }
	public static String summary() { return "native binds skipped=" + skipped + "/" + binds + "; terrain ranges=" + ranges + " -> " + mergedRanges; }
}
