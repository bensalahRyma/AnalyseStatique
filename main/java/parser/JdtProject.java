package parser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class JdtProject {
    public static class ParsedUnit {
        public final CompilationUnit cu;
        public final String source;
        public final Path path;
        public ParsedUnit(CompilationUnit cu, String source, Path path) {
            this.cu = cu; this.source = source; this.path = path;
        }
    }

    private final List<ParsedUnit> units = new ArrayList<>();
    public List<ParsedUnit> units() { return units; }

    public static JdtProject fromSourceDir(Path srcDir, List<String> classpath) throws Exception {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(srcDir)) {
            stream.filter(p -> p.toString().endsWith(".java"))
                    .forEach(files::add);
        }

        JdtProject project = new JdtProject();

        // Prepare compiler options for Java 21
        @SuppressWarnings("unchecked")
        var options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_21, options);

        for (Path p : files) {
            String code = Files.readString(p);

            ASTParser astParser = ASTParser.newParser(AST.JLS21);
            astParser.setKind(ASTParser.K_COMPILATION_UNIT);
            astParser.setResolveBindings(true);
            astParser.setBindingsRecovery(true);
            astParser.setEnvironment(
                    classpath.toArray(String[]::new),
                    new String[] { srcDir.toString() },
                    null,
                    /* includeRunningVMBootclasspath */ true
            );
            astParser.setUnitName(p.getFileName().toString()); // must end with .java
            astParser.setSource(code.toCharArray());
            astParser.setCompilerOptions(options);
            astParser.setStatementsRecovery(true);

            CompilationUnit cu = (CompilationUnit) astParser.createAST(null);
            project.units.add(new ParsedUnit(cu, code, p));
        }
        return project;
    }
}
