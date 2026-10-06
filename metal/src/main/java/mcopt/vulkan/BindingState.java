package mcopt.vulkan;

/** Tracks only native bindings, within ONE render pass/command buffer. Never skips frontend state changes. */
public final class BindingState {
	private boolean pipelineValid, indexValid;
	private long pipeline, indexBuffer, indexOffset;
	private int indexType;
	private final boolean[] vertexValid = new boolean[16];
	private final long[] vertexBuffers = new long[16], vertexOffsets = new long[16];
	public boolean pipeline(long handle) {
		if (pipelineValid && pipeline == handle) return false;
		pipelineValid = true; pipeline = handle; return true;
	}
	public boolean index(long buffer, long offset, int type) {
		if (indexValid && indexBuffer == buffer && indexOffset == offset && indexType == type) return false;
		indexValid = true; indexBuffer = buffer; indexOffset = offset; indexType = type; return true;
	}
	public void invalidateVertices(int first, int count) {
		for (int i = Math.max(0, first); i < vertexValid.length && i < (long) first + count; i++) vertexValid[i] = false;
	}
	public boolean vertex(int slot, long buffer, long offset) {
		if (slot < 0 || slot >= vertexValid.length) return true;
		if (vertexValid[slot] && vertexBuffers[slot] == buffer && vertexOffsets[slot] == offset) return false;
		vertexValid[slot] = true; vertexBuffers[slot] = buffer; vertexOffsets[slot] = offset; return true;
	}
}
