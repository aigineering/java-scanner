package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.util.*;

public class JavaSolutionParser {

    private final HashSet<INodeInfo> syntaxNodesSet = new HashSet<>();
    private final Dictionary<INodeInfo, Map<String, Object>> syntaxNodesInfo = new Hashtable<>();

    public Dictionary<INodeInfo, Map<String, Object>> getNodeInfos() {
        return syntaxNodesInfo;
    }

    public HashSet<INodeInfo> getNodesSet() {
        return syntaxNodesSet;
    }

    public boolean registerNode(INodeInfo nodeInfo) {
        if (syntaxNodesSet.add(nodeInfo)) {
            syntaxNodesInfo.put(nodeInfo, new HashMap<>());
            return true;
        }
        return false;
    }

    public void registerNodeData(INodeInfo nodeInfo, String key, Object value) {
        registerNode(nodeInfo);
        Map<String, Object> nodeData = syntaxNodesInfo.get(nodeInfo);
        if (nodeData != null) {
            nodeData.put(key, value);
        }
    }

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
    public List<INodeInfo> loadSyntaxNodes(String solutionPath) {
        List<INodeInfo> nodes = new ArrayList<>();
        File root = new File(solutionPath);
        if (root.isDirectory()) {
            List<File> javaFiles = new ArrayList<>();
            collectJavaFiles(root, javaFiles);
            for (File file : javaFiles) {
                try {
                    ParseResult<CompilationUnit> result = javaParser.parse(file);
                    if (result.isSuccessful() && result.getResult().isPresent()) {
                        CompilationUnit cu = result.getResult().get();
                        // Collect all nodes from the compilation unit
                        nodes.addAll(cu.findAll(Node.class).stream().map(
                                x -> {
                                    SyntaxNodeInfo nodeInfo = new SyntaxNodeInfo(x);
                                    registerNode(nodeInfo);
                                    return nodeInfo;
                                }).toList()
                        );
                    } else {
                        System.err.println("Failed to parse file " + file.getPath() + ": " + result.getProblems());
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
                    nodes.addAll(cu.findAll(Node.class).stream().map(
                            x -> {
                                SyntaxNodeInfo nodeInfo = new SyntaxNodeInfo(x);
                                registerNode(nodeInfo);
                                return nodeInfo;
                            }).toList()
                    );
                }
            } catch (Exception e) {
                System.err.println("Error parsing file " + root.getPath() + ": " + e.getMessage());
            }
        }
        nodes.stream().filter(x -> x instanceof SyntaxNodeInfo)
                .forEach(node -> extractSyntaxIndo((SyntaxNodeInfo) node));
        nodes.stream().filter(x -> x instanceof SyntaxNodeInfo)
                .forEach(node -> extractSymbols((SyntaxNodeInfo) node));
        nodes.stream().filter(x -> x instanceof SyntaxNodeInfo)
                .forEach(node -> extractDetailedSymbols((SyntaxNodeInfo) node));
        // Register all nodes in the syntaxNodesSet

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

    public void extractSyntaxIndo(SyntaxNodeInfo nodeInfo) {
        Node node = nodeInfo.node;
        Map<String, Object> extractSyntaxInfos = new HashMap<>();
        registerNodeData(nodeInfo, "nodeType", node.getClass().getSimpleName());
        registerNodeData(nodeInfo, "location", node.getRange().map(range -> range.begin.toString()).orElse("unknown"));
        registerNodeData(nodeInfo, "nodeText", node.toString());
        registerNodeData(nodeInfo, "nodeHash", Integer.toHexString(node.hashCode()));
        // Add file name and package declaration if available
        if (node instanceof CompilationUnit) {
            CompilationUnit cu = (CompilationUnit) node;
            registerNodeData(nodeInfo, "fileName", cu.getStorage().map(CompilationUnit.Storage::getFileName).orElse("unknown"));
            registerNodeData(nodeInfo, "packageDeclaration", cu.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse("default"));
        } else {
            registerNodeData(nodeInfo, "fileName", "N/A");
            registerNodeData(nodeInfo, "packageDeclaration", "N/A");
        }

    }

    private ResolvedDeclarationNodeInfo registerSymbol(ResolvedDeclaration resolved) {
        ResolvedDeclarationNodeInfo symbolNodeInfo = new ResolvedDeclarationNodeInfo(resolved);
        if (!registerNode(symbolNodeInfo)) {
            ;
            return symbolNodeInfo;
        }

        registerNodeData(symbolNodeInfo, "resolved_name", resolved.getName());
        registerNodeData(symbolNodeInfo, "referencedSymbol", resolved.getName());

        registerNodeData(symbolNodeInfo, "resolved_qualifiedSignature", getQualifiedSignature(resolved));
        if (resolved instanceof ResolvedMethodDeclaration) {
            var returnType = ((ResolvedMethodDeclaration) resolved).getReturnType();
            var returnSymbol = new ResolvedTypeNodeInfo(returnType);
            registerNodeData(symbolNodeInfo, "returnType", ((ResolvedMethodDeclaration) resolved).getReturnType());
        } else if (resolved instanceof ResolvedValueDeclaration) {
            registerNodeData(symbolNodeInfo, "valueType", ((ResolvedValueDeclaration) resolved).getType().describe());
        }
        return symbolNodeInfo;
    }

    private ResolvedTypeNodeInfo registerTypeSymbol(com.github.javaparser.resolution.types.ResolvedType type) {
        ResolvedTypeNodeInfo typeNodeInfo = new ResolvedTypeNodeInfo(type);
        if (!registerNode(typeNodeInfo)) {
            return typeNodeInfo;
        }
        registerNodeData(typeNodeInfo, "typeName", type.describe());
        registerNodeData(typeNodeInfo, "typeQualifiedName", type.asReferenceType().getQualifiedName());
        registerNodeData(typeNodeInfo, "typeHash", Integer.toHexString(type.hashCode()));
        registerNodeData(typeNodeInfo, "isReferenceType", type.isReferenceType());
        var typeDeclaration = type.asReferenceType().getTypeDeclaration();
        if (type.isReferenceType() && typeDeclaration.isPresent()) {
            registerSymbol(typeDeclaration.get());
        } else {
            registerNodeData(typeNodeInfo, "typeDeclaration", "N/A");
        }
        return typeNodeInfo;
    }

    // Extracts symbols for the given syntax node using the JavaParser symbol resolver.
    public void extractSymbols(SyntaxNodeInfo nodeInfo) {
        Node node = nodeInfo.node;
        Map<String, Object> symbolInfo = new HashMap<>();
        if (node instanceof Resolvable) {
            try {
                ResolvedDeclaration resolved = ((Resolvable<? extends ResolvedDeclaration>) node).resolve();
                registerSymbol(resolved);
            } catch (Exception e) {
                symbolInfo.put("error", e.getMessage());
            }
        } else {
            registerNodeData(nodeInfo, "extractSymbols_error", "Node is not resolvable");

        }
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
//            System.err.println("Error getting qualified signature for " + resolved.getName() + ": " + e.getMessage());
        }
        return "N/A";
    }

    // New method to extract detailed symbols (declared symbol, referenced symbol, return type, etc.)
    public Map<String, Object> extractDetailedSymbols(SyntaxNodeInfo nodeInfo) {
        Node node = nodeInfo.node;

        Map<String, Object> symbolDetails = new HashMap<>();
        if (node instanceof Resolvable) {
            try {
                ResolvedDeclaration resolved = (ResolvedDeclaration) ((Resolvable<?>) node).resolve();
                ResolvedDeclarationNodeInfo symbolNodeInfo = registerSymbol(resolved);

                registerNodeData(symbolNodeInfo, "declaredSymbol", resolved.getName());
                registerNodeData(symbolNodeInfo, "qualifiedSignature", getQualifiedSignature(resolved));
                if (resolved instanceof ResolvedMethodDeclaration) {
                    registerNodeData(symbolNodeInfo, "returnType", ((ResolvedMethodDeclaration) resolved).getReturnType().describe());
                } else if (resolved instanceof ResolvedValueDeclaration) {
                    registerNodeData(symbolNodeInfo, "valueType", ((ResolvedValueDeclaration) resolved).getType().describe());
                }
            } catch (Exception e) {
                symbolDetails.put("error", e.getMessage());
            }
        } else if (node instanceof NameExpr) {
            try {
                ResolvedDeclaration resolved = ((NameExpr) node).resolve();
                registerSymbol(resolved);


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
//            symbolDetails.put("error", "Node is not resolvable");

        }
        return symbolDetails;
    }
}
