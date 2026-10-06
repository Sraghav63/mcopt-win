package mcopt.vulkan;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.zip.CRC32;

/** Bounded, checksummed storage. Never send corrupt or another GPU/driver's data to Vulkan. */
public final class PipelineCacheStore {
	public static final int MAX_BYTES = 32 << 20;
	private static final int MAGIC = 0x4d43564b, ENVELOPE = 16;
	public record Identity(int vendor, int device, int driver, byte[] uuid) {
		public Identity { if (uuid.length != 16) throw new IllegalArgumentException("Vulkan cache UUID must be 16 bytes"); uuid = uuid.clone(); }
		@Override public byte[] uuid() { return uuid.clone(); }
		public String filename() {
			return "v1-" + Integer.toHexString(vendor) + "-" + Integer.toHexString(device) + "-" + Integer.toHexString(driver)
				+ "-" + HexFormat.of().formatHex(uuid) + ".bin";
		}
	}
	private PipelineCacheStore() { }
	public static boolean compatible(byte[] data, Identity identity) {
		if (data.length < 32 || data.length > MAX_BYTES) return false;
		var b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		int headerSize = b.getInt();
		if (headerSize != 32 || b.getInt() != 1 || b.getInt() != identity.vendor || b.getInt() != identity.device) return false;
		byte[] uuid = new byte[16]; b.get(uuid);
		return Arrays.equals(uuid, identity.uuid);
	}
	public static byte[] read(Path file, Identity identity) throws IOException {
		if (!Files.isRegularFile(file)) return null;
		if (Files.size(file) > MAX_BYTES + ENVELOPE) return null;
		byte[] bytes;
		try (var in = Files.newInputStream(file)) { bytes = in.readNBytes(MAX_BYTES + ENVELOPE + 1); }
		if (bytes.length < ENVELOPE + 32 || bytes.length > MAX_BYTES + ENVELOPE) return null;
		var b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		if (b.getInt() != MAGIC || b.getInt() != bytes.length - ENVELOPE) return null;
		long expectedCrc = b.getLong();
		byte[] data = Arrays.copyOfRange(bytes, ENVELOPE, bytes.length);
		var crc = new CRC32(); crc.update(data);
		return crc.getValue() == expectedCrc && compatible(data, identity) ? data : null;
	}
	public static void write(Path file, Identity identity, byte[] data) throws IOException {
		if (!compatible(data, identity)) throw new IOException("Invalid Vulkan pipeline cache header");
		Files.createDirectories(file.getParent());
		var crc = new CRC32(); crc.update(data);
		var bytes = ByteBuffer.allocate(ENVELOPE + data.length).order(ByteOrder.LITTLE_ENDIAN);
		bytes.putInt(MAGIC).putInt(data.length).putLong(crc.getValue()).put(data);
		Path temporary = Files.createTempFile(file.getParent(), ".mcopt-vulkan-", ".tmp");
		try {
			Files.write(temporary, bytes.array());
			try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
			catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
		} finally { Files.deleteIfExists(temporary); }
	}
}
