package mcopt.vulkan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Stable partition: preserve every fallback and its relative order. */
public final class BackendOrder {
	private BackendOrder() { }
	public static <T> List<T> prefer(List<T> backends, Predicate<T> preferred) {
		var first = new ArrayList<T>(); var rest = new ArrayList<T>();
		for (T backend : backends) (preferred.test(backend) ? first : rest).add(backend);
		first.addAll(rest); return first;
	}
}
