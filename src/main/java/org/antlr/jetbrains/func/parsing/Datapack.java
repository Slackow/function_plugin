package com.function.main;

import com.function.main.controlstuff.FunctionVisitor;
import com.function.main.grammar.FunctionLexer;
import com.function.main.grammar.MainFunctionParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

//TODO fix this
public class Datapack {
    private Map<String, List<String>> functionLines = new HashMap<>();

    private Map<String, List<String>> contents = new HashMap<>();
    private List<String> ticked = new ArrayList<>();
    private List<String> loaded = new ArrayList<>();


    private Path datapackPath;

    public Datapack(Path datapackPath, Collection<Path> paths) {
        this.datapackPath = datapackPath;
        paths.forEach(path -> {
            String namespace = pathToNamespace(datapackPath, path);
            try {
                contents.put(namespace, Files.readAllLines(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static String pathToNamespace(Path datapackPath, Path functionPath) {
        Path relativePath = datapackPath.resolve("data").relativize(functionPath);
        String s = relativePath.toString().replaceFirst("\\\\functions\\\\", ":").replace('\\', '/');
        assert s.endsWith(".mcfunction") || s.endsWith(".function");
        return s.substring(0, s.lastIndexOf('.'));
    }

    public void addLine(String namespace, String line) {
        functionLines.computeIfAbsent(namespace, key -> new ArrayList<>()).add(line);
    }

    public void addTicked(String namespace) {
        ticked.add(namespace);
    }

    public void addLoaded(String namespace) {
        loaded.add(namespace);
    }

    public void parse() {
        contents.forEach((namespace, lines) -> {
            String contents = String.join("\n", lines);
            if (contents.startsWith("/")) {
                contents = "\n" + contents;
                CompileStuff.LOGGER.warning("Errors will be one line off, add a blank line at the start of file to fix.");
            }
            CharStream in = CharStreams.fromString(contents);
            FunctionLexer functionLexer = new FunctionLexer(in);
            TokenStream tokens = new CommonTokenStream(functionLexer);
            MainFunctionParser parser = new MainFunctionParser(tokens);
            FunctionVisitor functionVisitor = new FunctionVisitor(this);
            functionVisitor.addNamespace(namespace);
            functionVisitor.visit(parser.program());
        });

    }

    public Map<String, Path> save() throws IOException {
        Map<String, Path> map = new HashMap<>();
        functionLines.forEach((namespace, fileContent) -> {
            fileContent.add(0, "#Generated with https://github.com/cowslayer7890/functional_mcfunctions");
            try {
                Path path = namespaceToPath(datapackPath, namespace);
                Files.createDirectories(path.getParent());
                Files.write(path, fileContent);
                map.put(namespace, path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if (!ticked.isEmpty()) {
            String content = ticked.stream()
                    .collect(Collectors.joining("\",\n\t\t", "{\n\t\"values\": [\n\t\t\"", "\"\n\t]\n}"));

            Path path = datapackPath.resolve("data/minecraft/tags/functions/tick.json");
            Files.createDirectories(path.getParent());
            Files.write(path, Collections.singleton(content));
        }
        if (!loaded.isEmpty()) {
            String content = loaded.stream()
                    .collect(Collectors.joining("\",\n\t\t", "{\n\t\"values\": [\n\t\t\"", "\"\n\t]\n}"));
            Path path = datapackPath.resolve("data/minecraft/tags/functions/load.json");
            Files.createDirectories(path.getParent());
            Files.write(path, Collections.singleton(content));
        }

        Path packMeta = datapackPath.resolve("pack.mcmeta");
        if (Files.notExists(packMeta)) {
            Files.write(packMeta, Arrays.asList(
                    "{",
                    "   \"pack\": {",
                    "       \"pack_format\": 1,",
                    "       \"description\": \"" + datapackPath.getFileName() + "\"",
                    "   }",
                    "}"
            ));
        }
        return map;
    }

    static Path namespaceToPath(Path datapackPath, String namespace) {
        assert datapackPath != null && namespace != null;
        return datapackPath.resolve(Paths.get("data", namespace.replace(":", "\\functions\\") + ".mcfunction"));
    }


    public Set<String> getNamespaces() {
        return functionLines.keySet();
    }

}
