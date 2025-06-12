package simplejavascanner;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.symbolsolver.*;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import com.github.javaparser.resolution.declarations.*;
import java.io.File;
import java.util.*;

public class JavaSolutionParser {

    private JavaParser javaParser;

    public JavaSolutionParser() {
        // Setup the symbol solver using a CombinedTypeSolver; add ReflectionTypeSolver for core JDK classes.
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration config = new ParserConfiguration().setSymbolResolver(symbolSolver);
        this.javaParser = new JavaParser(config);
    }

    // Recursively loads all syntax nodes from Java files located under the given solution path.
    public List<Node> loadSyntaxNodes(String solutionPath) {
        List<Node> nodes = new ArrayList<>();
        File root = new File(solutionPath);
        if (root.isDirectory()) {
            List<File> javaFiles = new ArrayList<>();
            collectJavaFiles(root, javaFiles);
            for (File file : javaFiles) {
                try {
                    ParseResult<CompilationUnit> result = javaParser.parse(file);
                    if (result.isSuccessful() && result.getResult().isPresent()) {
                        CompilationUnit cu = result.getResult().get();
                        nodes.addAll(cu.findAll(Node.class));
                    }
                } catch (Exception e) {
                    // ...existing code: handle parsing errors as required...
                }
            }
        }
        return nodes;
    }

    // Helper method to collect .java files recursively.
    private void collectJavaFiles(File dir, List<File> javaFiles) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                collectJavaFiles(file, javaFiles);
            } else if (file.getName().endsWith(".java")) {
                javaFiles.add(file);
            }
        }
    }

    // Extracts symbols for the given syntax node using the JavaParser symbol resolver.
    public Map<String, Object> extractSymbols(Node node) {
        Map<String, Object> symbolInfo = new HashMap<>();
        try {
            ResolvedDeclaration resolved = node.resolve();
            symbolInfo.put("name", resolved.getName());
            symbolInfo.put("qualifiedSignature", getQualifiedSignature(resolved));
        } catch (Exception e) {
            symbolInfo.put("error", e.getMessage());
        }
        return symbolInfo;
    }

    // Helper method to obtain a qualified signature or description from the resolved declaration.
    private String getQualifiedSignature(ResolvedDeclaration resolved) {
        try {
            if (resolved instanceof ResolvedMethodDeclaration) {
                return ((ResolvedMethodDeclaration) resolved).getQualifiedSignature();
            } else if (resolved instanceof ResolvedReferenceTypeDeclaration) {
                return ((ResolvedReferenceTypeDeclaration) resolved).getQualifiedName();
            } else if (resolved instanceof ResolvedValueDeclaration) {
                return ((ResolvedValueDeclaration) resolved).getType().describe();
            }
        } catch (Exception e) {
            // ...existing code: handle exceptions silently...
        }
        return "N/A";
    }
}
