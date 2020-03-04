package io.github.upcraftlp.entitymodelmapper;

import com.google.common.collect.Lists;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EntityModelMapper {

    private static final Logger logger = LoggerFactory.getLogger("EntityModelMapper");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\W(?<fqcn>(?:\\w+\\.)*(?:\\w+));");
    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile("new\\W(?<class>\\w+)\\(.*\\)");
    private static final Pattern UNOPTIMIZED_CLASS_PATTERN = Pattern.compile("(\\w+)");
    private static final Pattern METHOD_PATTERN = Pattern.compile("(?:\\W+|\\.)(?<method>\\w+)\\(.*\\)");
    private static final Pattern FIELD_PATTERN = Pattern.compile("\\.(?<field>\\w+)(?:\\W=\\w+;)|\\.");

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
                        Remapper.mapClassName(fqcn, true).map(s -> s.replaceAll("/", ".")).ifPresent(s -> sb.replace(importMatcher.start("fqcn"), importMatcher.end("fqcn"), s));
                    }
                }
                Matcher constructorMatcher = CONSTRUCTOR_PATTERN.matcher(sb.toString());
                while(constructorMatcher.find()) {
                    String className = constructorMatcher.group("class");
                    if(className != null) {
                        sb.replace(constructorMatcher.start("class"), constructorMatcher.end("class"), Remapper.mapClassName(className, false).orElseThrow(() -> new NoSuchElementException("constructor mapping not found for " + className)));
                    }
                }
                Matcher classMatcher = UNOPTIMIZED_CLASS_PATTERN.matcher(sb.toString());
                while(classMatcher.find()) {
                    String clazz = classMatcher.group();
                    String mapped = Remapper.mapClassName(clazz, false).orElseGet(() -> Remapper.mapField(clazz));
                    if(mapped != null) {
                        sb.replace(classMatcher.start(), classMatcher.end(), mapped);
                    }
                }
                Matcher fieldMatcher = FIELD_PATTERN.matcher(sb.toString());
                while(fieldMatcher.find()) {
                    String field = fieldMatcher.group("field");
                    if(field != null) {
                        String mapped = Remapper.mapField(field);
                        if(mapped != null) {
                            sb.replace(fieldMatcher.start("field"), fieldMatcher.end("field"), mapped);
                        }
                    }
                }
                Matcher methodMatcher = METHOD_PATTERN.matcher(sb.toString());
                while(methodMatcher.find()) {
                    String method = methodMatcher.group("method");
                    if(method != null) {
                        String mapped = Remapper.mapMethod(method);
                        if(mapped != null) {
                            sb.replace(methodMatcher.start("method"), methodMatcher.end("method"), mapped);
                        }
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
