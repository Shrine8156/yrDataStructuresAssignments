import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class RulesTest {

        private static final List<String> BANNED = List.of(
            "com.fasterxml", "org.codehaus.jackson", "com.google.gson", "org.json", "javax.json",
            "jakarta.json", "org.glassfish.json", "org.eclipse.parsson", "org.apache.johnzon",
            "net.minidev", "com.alibaba", "com.squareup.moshi", "com.github.cliftonlabs",
            "com.jayway", "org.yaml", "kotlinx.serialization", "javax.script", "jdk.nashorn",
            "org.graalvm", "java.util.regex", "ScriptEngine");

    private static String stripComments(String source) {
        String noBlock = source.replaceAll("(?s)/\\*.*?\\*/", " ");
        return noBlock.replaceAll("(?m)//.*$", " ");
    }

    @Test
    @DisplayName("rules: no JSON library, script engine, or regex in src/main/java")
    void noJsonLibrariesInSource() throws IOException {
        List<Path> javaFiles;
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            javaFiles = files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles) {
            String code = stripComments(Files.readString(file));
            for (String banned : BANNED) {
                if (code.contains(banned)) {
                    violations.add(file + " uses \"" + banned + "\"");
                }
            }
        }
        assertTrue(!javaFiles.isEmpty(), "no source files found under src/main/java");
        assertTrue(violations.isEmpty(), "Forbidden JSON library or parsing shortcut found: " + violations);
    }

    @Test
    @DisplayName("rules: build.gradle has no dependencies other than JUnit")
    void noExtraDependencies() throws IOException {
        String gradle = stripComments(Files.readString(Path.of("build.gradle")));
        Pattern dependencyLine = Pattern.compile(
                "^\\s*(implementation|api|compileOnly|runtimeOnly|annotationProcessor|testImplementation|"
                        + "testCompileOnly|testRuntimeOnly)\\b(.*)$",
                Pattern.MULTILINE);
        List<String> violations = new ArrayList<>();
        Matcher m = dependencyLine.matcher(gradle);
        while (m.find()) {
            String line = m.group(0).trim();
            if (!line.contains("org.junit")) {
                violations.add(line);
            }
        }
        for (String banned : BANNED) {
            if (gradle.contains(banned)) {
                violations.add("build.gradle mentions \"" + banned + "\"");
            }
        }
        assertTrue(violations.isEmpty(), "build.gradle may only depend on JUnit. Found: " + violations);
    }

    @Test
    @DisplayName("rules: the test files and data files are still present")
    void testsAndDataStillPresent() {
        for (String required : List.of("data/logs.json", "data/logs.min.json", "data/example.json",
                "data/scalar.json", "src/test/java/TestUtil.java")) {
            assertTrue(Files.exists(Path.of(required)), "missing required file: " + required);
        }
    }

    @Test
    @DisplayName("rules: JsonParser builds objects with a HashMap and tracks keys with a HashSet")
    void usesHashMapAndHashSet() throws IOException {
        String code = stripComments(Files.readString(Path.of("src/main/java/JsonParser.java")));
        assertTrue(code.contains("new HashMap"), "parseObject must create a HashMap");
        assertTrue(code.contains("new HashSet"), "parseObject must create a HashSet");
        assertTrue(code.contains("new ArrayList"), "parseArray must create an ArrayList");
    }
}
