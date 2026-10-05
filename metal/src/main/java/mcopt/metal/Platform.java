package mcopt.metal;

/** Platform capabilities, safe to query before Minecraft or native code loads. */
public final class Platform {
	private Platform() { }
	public static boolean windows() { return System.getProperty("os.name", "").startsWith("Windows"); }
	public static boolean metalAvailable() {
		String arch = System.getProperty("os.arch", "");
		return System.getProperty("os.name", "").startsWith("Mac") && (arch.equals("aarch64") || arch.equals("arm64"))
			&& Platform.class.getResource("/natives/libmcmetal.dylib") != null;
	}
	public static boolean metalEnabled() {
		return metalAvailable() && Boolean.parseBoolean(System.getProperty("mcopt.metal", "true"));
	}
}
