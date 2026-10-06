package mcopt.vulkan;

import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.junit.Assert.*;

/** Verify hooks against the actual Minecraft/Sodium binaries, including the direct multi-draw path unavailable on some GPUs. */
public class VulkanMixinContractTest {
	private static ClassNode read(String name) throws Exception {
		try (InputStream in = VulkanMixinContractTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
			assertNotNull("Missing target " + name, in);
			ClassNode node = new ClassNode(); new ClassReader(in).accept(node, 0); return node;
		}
	}
	private static Object value(AnnotationNode a, String key) {
		for (int i = 0; i < a.values.size(); i += 2) if (key.equals(a.values.get(i))) return a.values.get(i + 1);
		return null;
	}
	@Test public void everyVulkanHookMatchesItsShippingTarget() throws Exception {
		for (String name : new String[] {"GraphicsApiMixin", "VulkanDeviceMixin", "VulkanPipelineMixin", "VulkanRenderPassMixin", "VulkanMultiDrawBatchMixin", "VulkanIndirectBatchMixin"}) {
			ClassNode mixin = read("mcopt/vulkan/mixin/" + name);
			AnnotationNode annotation = mixin.invisibleAnnotations.stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
			Type type = (Type) ((List<?>) value(annotation, "value")).getFirst();
			ClassNode target = read(type.getInternalName());
			for (var method : mixin.methods) {
				if (method.visibleAnnotations == null) continue;
				for (var hook : method.visibleAnnotations) {
					if (!hook.desc.endsWith("/Inject;") && !hook.desc.endsWith("/Redirect;")) continue;
					var selectors = (List<?>) value(hook, "method");
					AnnotationNode at = value(hook, "at") instanceof List<?> list ? (AnnotationNode) list.getFirst() : (AnnotationNode) value(hook, "at");
					int matches = 0;
					for (var actual : target.methods) {
						if (!selectors.contains(actual.name)) continue;
						if (!"INVOKE".equals(value(at, "value"))) { matches++; continue; }
						String invocation = (String) value(at, "target");
						for (var instruction : actual.instructions) if (instruction instanceof MethodInsnNode call)
							if (invocation.equals("L" + call.owner + ";" + call.name + call.desc)) matches++;
					}
					int required = value(hook, "require") instanceof Integer n ? n : 1;
					assertTrue(name + "." + method.name + " matched " + matches + " times, needs " + required, matches >= required);
				}
			}
		}
	}
}
