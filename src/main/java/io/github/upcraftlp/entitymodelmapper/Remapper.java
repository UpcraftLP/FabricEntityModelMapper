package io.github.upcraftlp.entitymodelmapper;

import com.google.common.collect.ImmutableMap;
import io.github.upcraftlp.entitymodelmapper.mappings.MappingsHelper;
import io.github.upcraftlp.entitymodelmapper.mappings.Namespace;
import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.mappings.Mappings;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class Remapper {

    private static final Mappings mappings = MappingsHelper.prepareMappings();
    private static final Map<String, String> VOLDE_CLASSES_FQCN = ImmutableMap.<String, String>builder()
            .put("net/minecraft/client/model/ModelBox", "net/minecraft/class_630$class_628") //yarn: ModelPart$Cuboid
            .put("net/minecraft/client/model/ModelBase", "net/minecraft/class_583") //yarn: EntityModel
            .put("net/minecraft/client/model/ModelRenderer", "net/minecraft/class_630") //yarn: ModelPart
            .put("net/minecraft/entity/Entity", "net/minecraft/class_1297") //yarn: Entity
            .build();
    private static final Map<String, String> VOLDE_FIELDS = ImmutableMap.<String, String>builder()
            .put("textureWidth", "field_17138")
            .put("textureHeight", "field_17139")
            .put("cubeList", "field_3663")
            .put("rotateAngleX", "field_3654")
            .put("rotateAngleY", "field_3675")
            .put("rotateAngleZ", "field_3674")
            .build();
    private static final Map<String, String> VOLDE_METHODS = ImmutableMap.<String, String>builder()
            .put("setRotationPoint", "method_2851") //yarn: setPivot
            .put("addChild", "method_2845") //yarn: addChild
            .put("render", "method_2828") //yarn: render
            .build();
    private static Function<String, String> SUBSTRING_FUNC = s -> {
        if(s.contains("$")) {
            return s.substring(s.lastIndexOf('$') + 1);
        }
        else {
            return s.substring(s.lastIndexOf('/') + 1);
        }
    };
    private static final Map<String, String> VOLDE_CLASSES = VOLDE_CLASSES_FQCN.entrySet().stream().collect(ImmutableMap.toImmutableMap(e -> SUBSTRING_FUNC.apply(e.getKey()), Map.Entry::getValue));

    @Nullable
    public static String mapField(String fieldName) {
        String lookup = VOLDE_FIELDS.get(fieldName);
        return lookup == null ? null : mappings.getFieldEntries().stream().filter(it -> it.get(Namespace.INTERMEDIARY).getName().equals(lookup)).map(it -> it.get(Namespace.YARN)).map(EntryTriple::getName).findAny().orElse(null);
    }

    @Nullable
    public static String mapMethod(String methodName) {
        String lookup = VOLDE_METHODS.get(methodName);
        return lookup == null ? null : mappings.getMethodEntries().stream().filter(it -> it.get(Namespace.INTERMEDIARY).getName().equals(lookup)).map(it -> it.get(Namespace.YARN)).map(EntryTriple::getName).findAny().orElse(null);
    }

    public static Optional<String> mapClassName(String clazz, boolean fqcn) {
        String clazz2 = clazz.replaceAll("\\.", "/");
        return Optional.ofNullable(fqcn ? VOLDE_CLASSES_FQCN.get(clazz2) : VOLDE_CLASSES.get(clazz2))
                .flatMap(intermediary -> mappings.getClassEntries().stream()
                        .filter(classEntry -> classEntry.get(Namespace.INTERMEDIARY).equals(intermediary))
                        .map(classEntry -> classEntry.get(Namespace.YARN))
                        .map(mapped -> fqcn ? mapped.replaceAll("\\$", ".") : SUBSTRING_FUNC.apply(mapped))
                        .findAny());
    }
}
