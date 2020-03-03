package io.github.upcraftlp.entitymodelmapper;

import com.google.common.collect.Lists;
import io.github.upcraftlp.entitymodelmapper.mappings.MappingsHelper;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.fabricmc.mappings.Mappings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EntityModelMapper {


    private static final Logger logger = LoggerFactory.getLogger("EntityModelMapper");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\W(?<fqcn>(?:\\w+\\.)*(?:\\w+));");
    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile("new\\W(?<class>\\w+)\\(.*\\)");
    //private static final Pattern CLASS_PATTERN = Pattern.compile("\\w+");
    private static final Pattern METHOD_PATTERN = Pattern.compile("(?!new)(?<method>\\w+)\\(.*\\)");
    private static final Pattern FIELD_PATTERN = Pattern.compile("\\.(?<field>\\w+)\\W=\\w+;");
    private static final Mappings mappings = MappingsHelper.prepareMappings();

    public static void main(String[] args) {
        OptionParser parser = new OptionParser();
        OptionSpec<File> inputPathSpec = parser.acceptsAll(Lists.newArrayList("i", "input"), "the input file").withRequiredArg().ofType(File.class);
        OptionSpec<File> outputPathSpec = parser.acceptsAll(Lists.newArrayList("o", "output"), "the output file").withRequiredArg().ofType(File.class);
        OptionSet options = parser.parse(args);
        Path inputPath = inputPathSpec.value(options).toPath();
        Path outputPath = outputPathSpec.value(options).toPath();
        logger.info("input: {}", inputPath.toAbsolutePath());
        logger.info("output: {}", outputPath.toAbsolutePath());
        try(Stream<String> inLines = Files.lines(inputPath, StandardCharsets.UTF_8); BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            inLines.map(line -> {
                StringBuilder sb = new StringBuilder(line);
                Matcher importMatcher = IMPORT_PATTERN.matcher(line);
                while(importMatcher.find()) {
                    String fqcn = importMatcher.group("fqcn");
                    if(fqcn != null) {
                        sb.replace(importMatcher.start("fqcn"), importMatcher.end("fqcn"), Remapper.mapClassName(mappings, fqcn, true).replaceAll("/", "."));
                    }
                }
                Matcher constructorMatcher = CONSTRUCTOR_PATTERN.matcher(sb.toString());
                while(constructorMatcher.find()) {
                    String className = constructorMatcher.group("class");
                    if(className != null) {
                        sb.replace(constructorMatcher.start("class"), constructorMatcher.end("class"), Remapper.mapClassName(mappings, className, false));
                    }
                }


                return sb.toString();
            })
                    .forEachOrdered(s -> {
                        try {
                            writer.write(s);
                            writer.newLine();
                        }
                        catch (IOException e) {
                            logger.error("failed to write to file: ", e);
                        }
                    });
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
