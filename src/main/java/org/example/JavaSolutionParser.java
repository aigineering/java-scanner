package org.example;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.NameExpr; // added import
import com.github.javaparser.symbolsolver.*;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.Resolvable;
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
                    System.err.println("Error parsing file " + file.getPath() + ": " + e.getMessage());
                }
            }
        } else if (root.isFile() && root.getName().endsWith(".java")) {
            try {
                ParseResult<CompilationUnit> result = javaParser.parse(root);
                if (result.isSuccessful() && result.getResult().isPresent()) {
                    CompilationUnit cu = result.getResult().get();
                    nodes.addAll(cu.findAll(Node.class));
                }
            } catch (Exception e) {
                System.err.println("Error parsing file " + root.getPath() + ": " + e.getMessage());
            }
        }
        return nodes;
    }

    // Helper method to collect .java files recursively.
    private void collectJavaFiles(File dir, List<File> javaFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectJavaFiles(file, javaFiles);
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }
    }

    // Extracts symbols for the given syntax node using the JavaParser symbol resolver.
    public Map<String, Object> extractSymbols(Node node) {
        Map<String, Object> symbolInfo = new HashMap<>();
        if (node instanceof Resolvable) {
            try {
                ResolvedDeclaration resolved = ((Resolvable<? extends ResolvedDeclaration>) node).resolve();
                symbolInfo.put("name", resolved.getName());
                symbolInfo.put("qualifiedSignature", getQualifiedSignature(resolved));
            } catch (Exception e) {
                symbolInfo.put("error", e.getMessage());
            }
        } else {
            symbolInfo.put("error", "Node is not resolvable");
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
            System.err.println("Error getting qualified signature for " + resolved.getName() + ": " + e.getMessage());
        }
        return "N/A";
    }

    // New method to extract detailed symbols (declared symbol, referenced symbol, return type, etc.)
    public Map<String, Object> extractDetailedSymbols(Node node) {
        Map<String, Object> symbolDetails = new HashMap<>();
        if (node instanceof Resolvable) {
            try {
                ResolvedDeclaration resolved = (ResolvedDeclaration) ((Resolvable<?>) node).resolve();
                symbolDetails.put("declaredSymbol", resolved.getName());
                symbolDetails.put("qualifiedSignature", getQualifiedSignature(resolved));
                if (resolved instanceof ResolvedMethodDeclaration) {
                    symbolDetails.put("returnType", ((ResolvedMethodDeclaration) resolved).getReturnType().describe());
                } else if (resolved instanceof ResolvedValueDeclaration) {
                    symbolDetails.put("valueType", ((ResolvedValueDeclaration) resolved).getType().describe());
                }
            } catch (Exception e) {
                symbolDetails.put("error", e.getMessage());
            }
        } else if (node instanceof NameExpr) {
            try {
                ResolvedDeclaration resolved = ((NameExpr) node).resolve();
                symbolDetails.put("referencedSymbol", resolved.getName());
                symbolDetails.put("qualifiedSignature", getQualifiedSignature(resolved));
            } catch (Exception e) {
                symbolDetails.put("error", e.getMessage());
            }
        } else if (node instanceof com.github.javaparser.ast.expr.MethodCallExpr) { // New branch for MethodCallExpr
            try {
                ResolvedDeclaration resolved = ((com.github.javaparser.ast.expr.MethodCallExpr) node).resolve();
                symbolDetails.put("methodCall", resolved.getName());
                symbolDetails.put("qualifiedSignature", getQualifiedSignature(resolved));
            } catch (Exception e) {
                symbolDetails.put("error", e.getMessage());
            }
        } else {
            symbolDetails.put("error", "Node is not resolvable");
        }
        return symbolDetails;
    }
}
