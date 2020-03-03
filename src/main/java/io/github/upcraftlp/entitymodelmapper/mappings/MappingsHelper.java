package io.github.upcraftlp.entitymodelmapper.mappings;

import com.google.common.base.Preconditions;
import io.github.upcraftlp.entitymodelmapper.EntityModelMapper;
import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.model.V2MappingsProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MappingsHelper {

    private static final String MAPPINGS_PATH = "mappings/mappings.tiny";
    private static final Logger logger = LoggerFactory.getLogger("Mappings");

    @NotNull
    public static Mappings prepareMappings() {
        Mappings mappings = null;
        logger.info("loading tiny mappings file");
        InputStream inputStream = EntityModelMapper.class.getClassLoader().getResourceAsStream(MAPPINGS_PATH);
        Preconditions.checkNotNull(inputStream, "no mappings available!");
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            mappings = V2MappingsProvider.readTinyMappings(reader);
        }
        catch (IOException e) {
            logger.error("failed to load mappings", e);
        }
        if(mappings == null) {
            throw new IllegalStateException("unable to load mappings!");
        }
        logger.info("mapping namespaces loaded: {}", String.join(", ", mappings.getNamespaces()));
        return mappings;
    }
}
