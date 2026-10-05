package mcopt.metal;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/** Windows thread scheduling; keeps process priority normal and never requests realtime priority.
	* https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-setthreadpriority
	*/
final class WindowsQos {
	private static MethodHandle currentThread, setPriority;
	private static boolean unavailable;
	private WindowsQos() { }

	static int priority(int qos) {
		return switch (qos) {
			case 0x09 -> -2; // background -> lowest
			case 0x11 -> -1; // utility -> below normal
			case 0x19, 0x21 -> 1; // initiated / interactive -> above normal
			default -> 0;
		};
	}

	static synchronized void self(String role, int qos) {
		if (unavailable) return;
		try {
			if (setPriority == null) {
				var lib = SymbolLookup.libraryLookup("Kernel32.dll", Arena.global());
				var linker = Linker.nativeLinker();
				currentThread = linker.downcallHandle(lib.find("GetCurrentThread").orElseThrow(), FunctionDescriptor.of(ADDRESS));
				setPriority = linker.downcallHandle(lib.find("SetThreadPriority").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
			}
			MemorySegment thread = (MemorySegment) currentThread.invokeExact();
			int value = priority(qos);
			int ok = (int) setPriority.invokeExact(thread, value);
			if (ok == 0) throw new IllegalStateException("SetThreadPriority failed");
			if (Boolean.getBoolean("mcopt.qos.log")) System.out.println("mcopt: Windows thread priority " + value + " for " + role);
		} catch (Throwable t) {
			unavailable = true;
			System.err.println("mcopt: Windows thread scheduling unavailable; keeping default priorities: " + t);
		}
	}
}
