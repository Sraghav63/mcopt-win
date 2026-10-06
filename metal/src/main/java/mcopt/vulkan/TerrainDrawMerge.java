package mcopt.vulkan;

import java.nio.ByteBuffer;

/** Compact adjacent Sodium quad ranges, preserving triangle and instance order.
 * Direct layout: VkMultiDrawIndexedInfoEXT {firstIndex, indexCount, vertexOffset}.
 * Indirect layout: VkDrawIndexedIndirectCommand {indexCount, instanceCount, firstIndex, vertexOffset, firstInstance}.
 * Never changes the original batch. Bounded to Sodium's minimum shared quad index buffer (16384 quads).
 */
public final class TerrainDrawMerge {
	private static final long MAX_INDICES = 16384L * 6;
	private TerrainDrawMerge() { }
	public static int merge(ByteBuffer input, int draws, boolean indirect, ByteBuffer output) {
		int stride = indirect ? 20 : 12, countOffset = indirect ? 0 : 4, firstOffset = indirect ? 8 : 0, baseOffset = indirect ? 12 : 8;
		if (draws < 0 || draws > input.remaining() / stride || draws > output.remaining() / stride) throw new IllegalArgumentException("Draw buffer bounds");
		int start = input.position(), dest = output.position(), out = 0;
		for (int i = 0; i < draws; i++) {
			int next = start + i * stride, previous = dest + (out - 1) * stride;
			long count = Integer.toUnsignedLong(input.getInt(next + countOffset));
			if (out > 0 && count > 0 && count % 6 == 0 && input.getInt(next + firstOffset) == 0 && output.getInt(previous + firstOffset) == 0) {
				long oldCount = Integer.toUnsignedLong(output.getInt(previous + countOffset));
				long followingVertex = (long) output.getInt(previous + baseOffset) + oldCount / 6 * 4;
				boolean singleInstance = !indirect || (input.getInt(next + 4) == 1 && input.getInt(next + 16) == 0
					&& output.getInt(previous + 4) == 1 && output.getInt(previous + 16) == 0);
				if (singleInstance && oldCount > 0 && oldCount % 6 == 0 && oldCount + count <= MAX_INDICES
					&& followingVertex == input.getInt(next + baseOffset)) {
					output.putInt(previous + countOffset, (int) (oldCount + count));
					continue;
				}
			}
			for (int word = 0; word < stride; word += 4) output.putInt(dest + out * stride + word, input.getInt(next + word));
			out++;
		}
		return out;
	}
}
