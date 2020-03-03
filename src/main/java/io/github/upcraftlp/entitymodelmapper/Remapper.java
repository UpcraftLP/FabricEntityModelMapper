package io.github.upcraftlp.entitymodelmapper;

import com.google.common.collect.ImmutableMap;
import io.github.upcraftlp.entitymodelmapper.mappings.Namespace;
import net.fabricmc.mappings.Mappings;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class Remapper {

    private static final Map<String, String> VOLDE_CLASSES_FQCN = ImmutableMap.<String, String>builder()
            .put("net/minecraft/client/model/ModelBox", "net/minecraft/class_630$class_628") //yarn: ModelPart$Cuboid
            .put("net/minecraft/client/model/ModelBase", "net/minecraft/class_583") //yarn: EntityModel
            .put("net/minecraft/client/model/ModelRenderer", "net/minecraft/class_630") //yarn: ModelPart
            .put("net/minecraft/entity/Entity", "net/minecraft/class_1297") //yarn: Entity
            .build();
    private static Function<String, String> SUBSTRING_FUNC = s -> {
        if(s.contains("$")) {
            return s.substring(s.lastIndexOf('$') + 1);
        }
        else return s.substring(s.lastIndexOf('/') + 1);
    };
    private static final Map<String, String> VOLDE_CLASSES = VOLDE_CLASSES_FQCN.entrySet().stream().collect(ImmutableMap.toImmutableMap(e -> SUBSTRING_FUNC.apply(e.getKey()), Map.Entry::getValue));

    public static String mapClassName(Mappings mappings, String clazz, boolean fqcn) {
        String clazz2 = clazz.replaceAll("\\.", "/");
        String intermediary = fqcn ? VOLDE_CLASSES_FQCN.get(clazz2) : VOLDE_CLASSES.get(clazz2);
        String mapped = mappings.getClassEntries().stream().filter(classEntry -> classEntry.get(Namespace.INTERMEDIARY).equals(intermediary))
                .map(classEntry -> classEntry.get(Namespace.YARN)).findAny().orElseThrow(() -> new NoSuchElementException("no mappings found for " + clazz));
        return fqcn ? mapped.replaceAll("\\$", ".") : SUBSTRING_FUNC.apply(mapped);
    }
}
