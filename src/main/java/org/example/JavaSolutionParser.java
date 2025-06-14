package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.resolution.MethodAmbiguityException;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JavaSolutionParser {

    private final NodeInfoTracker nodeInfoTracker = new NodeInfoTracker();
    private final RelationshupTracker relationshipTracker = new RelationshupTracker();

    //    private final HashSet<INodeInfo> syntaxNodesSet = new HashSet<>();
    private final Dictionary<INodeInfo, Map<String, Object>> syntaxNodesInfo = new Hashtable<>();
    private final Map<GraphRelationship, Map<String, Object>> relationshipMapHashtable = new Hashtable<>();

    public Dictionary<INodeInfo, Map<String, Object>> getNodeInfos() {
        return syntaxNodesInfo;
    }

    public Map<GraphRelationship, Map<String, Object>> relationshipData() {
        return relationshipMapHashtable;
    }

    public HashSet<INodeInfo> getNodesSet() {

        return nodeInfoTracker.getAllNodes();
    }

    public HashSet<GraphRelationship> getRelationships() {
        return relationshipTracker.getAllRelationships();
    }


    public void registerNodeData(INodeInfo nodeInfo, String key, Object value) {
        this.nodeInfoTracker.registerNode(nodeInfo);
        Map<String, Object> nodeData = syntaxNodesInfo.get(nodeInfo);
        if (nodeData == null) {
            nodeData = new HashMap<>();
            syntaxNodesInfo.put(nodeInfo, nodeData);
        }

        nodeData.put(key, value);
    }

    public void registerRelationshipData(GraphRelationship relationship, String key, Object value) {
        this.relationshipTracker.registerRelationship(relationship);
        Map<String, Object> relationData = relationshipMapHashtable.computeIfAbsent(relationship, k -> new HashMap<>());

        relationData.put(key, value);
    }

    private final JavaParser javaParser;

    private String baseSourcePath;

    public JavaSolutionParser(String baseSourcePath) throws XmlPullParserException, IOException {
        // Setup the symbol solver using a CombinedTypeSolver; add ReflectionTypeSolver for core JDK classes.
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        this.baseSourcePath = baseSourcePath;
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(new File("src/main/java"))); // your source root
        SolverUtils.registerJarsFromPom(typeSolver, baseSourcePath + "/pom.xml");
//        typeSolver.add(new MavenTypeSolver(baseSourcePath, true));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration config = new ParserConfiguration().setSymbolResolver(symbolSolver);
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        this.javaParser = new JavaParser(config);
    }

    // Recursively loads all syntax nodes from Java files located under the given solution path.
    public void loadSyntaxNodes() {
        File root = new File(baseSourcePath);
        if (root.isDirectory()) {
            List<File> javaFiles = new ArrayList<>();
            collectJavaFiles(root, javaFiles);
            for (File file : javaFiles) {
                try {
                    ParseResult<CompilationUnit> result = javaParser.parse(file);
                    if (result.isSuccessful() && result.getResult().isPresent()) {
                        CompilationUnit cu = result.getResult().get();
                        cu.stream().forEach(this::registerSyntaxNode);

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
                    cu.stream().forEach(this::registerSyntaxNode);

                }
            } catch (Exception e) {
                System.err.println("Error parsing file " + root.getPath() + ": " + e.getMessage());
            }
        }

    }

    public void enrichAndDiscoverMoreNodes() {
        // Iterate through all syntax nodes and extract symbols and detailed symbols.
        var newNodes = nodeInfoTracker.getNewNodes();
        while (!newNodes.isEmpty()) {
            System.out.println("Found new nodes: " + newNodes.size());
            nodeInfoTracker.clearNewNodes();
            for (INodeInfo nodeInfo : newNodes) {
                if (nodeInfo instanceof SyntaxNodeInfo syntaxNodeInfo) {
                    describeSyntaxInfo(syntaxNodeInfo);
                    addParentToNode(syntaxNodeInfo);
                    extractSymbols(syntaxNodeInfo);
                    extractDetailedSymbols(syntaxNodeInfo);

                } else if (nodeInfo instanceof ResolvedDeclarationNodeInfo resolvedNodeInfo) {
                    describeSymbolNode(resolvedNodeInfo);
                } else if (nodeInfo instanceof ResolvedTypeNodeInfo typeNodeInfo) {
                    describeTypeSymbol(typeNodeInfo);
                }
            }
            newNodes = nodeInfoTracker.getNewNodes();
        }
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


    private SyntaxNodeInfo registerSyntaxNode(Node node) {

        SyntaxNodeInfo nodeInfo = new SyntaxNodeInfo(node);
        nodeInfoTracker.registerNode(nodeInfo);
        registerNodeData(nodeInfo, "registered_as", "syntaxNode");
        return nodeInfo;
    }

    private ResolvedDeclarationNodeInfo registerSymbol(ResolvedDeclaration resolved) {
        ResolvedDeclarationNodeInfo symbolNodeInfo = new ResolvedDeclarationNodeInfo(resolved);
        if (!nodeInfoTracker.registerNode(symbolNodeInfo)) {

            return symbolNodeInfo;
        }

        return symbolNodeInfo;
    }

    private ResolvedTypeNodeInfo registerTypeSymbol(ResolvedType type) {
        ResolvedTypeNodeInfo typeNodeInfo = new ResolvedTypeNodeInfo(type);
        if (!nodeInfoTracker.registerNode(typeNodeInfo)) {
            return typeNodeInfo;
        }

        return typeNodeInfo;
    }


    public void describeSymbolNode(ResolvedDeclarationNodeInfo nodeInfo) {
        var resolved = nodeInfo.getDeclaration();
        ResolvedDeclarationNodeInfo symbolNodeInfo = new ResolvedDeclarationNodeInfo(resolved);

        registerNodeData(symbolNodeInfo, "registered_as", "resolve_declaration");

        registerNodeData(symbolNodeInfo, "resolved_name", resolved.getName());
        registerNodeData(symbolNodeInfo, "referencedSymbol", resolved.getName());

        registerNodeData(symbolNodeInfo, "resolved_qualifiedSignature", getQualifiedSignature(resolved));
        if (resolved instanceof ResolvedMethodDeclaration) {
            try {
                var returnType = ((ResolvedMethodDeclaration) resolved).getReturnType();
                var target = registerTypeSymbol(returnType);
                var relationship = new GraphRelationship(symbolNodeInfo, target, "return_type_of");
                registerRelationshipData(relationship, "uses", "return_type");
                registerNodeData(symbolNodeInfo, "returnType", ((ResolvedMethodDeclaration) resolved).getReturnType().toString());
            } catch (UnsolvedSymbolException exception) {

                registerNodeData(symbolNodeInfo, "returnType", "UnsolvedSymbolException: " + exception.getMessage());
                System.err.println("UnsolvedSymbolException for return type in " + resolved.getName() + ": " + exception.getMessage() + " For node :" + nodeInfo);
            }
        } else if (resolved instanceof ResolvedValueDeclaration resolvedValueDeclaration) {
            try {
                var resType = resolvedValueDeclaration.getType();
                var target = registerTypeSymbol(resType);
                var relationship = new GraphRelationship(symbolNodeInfo, target, "value_type_of");
                registerRelationshipData(relationship, "uses", "value_type");
                registerNodeData(symbolNodeInfo, "valueType", resType.describe());
            } catch (MethodAmbiguityException e) {
//                throw new RuntimeException(e);
                registerNodeData(symbolNodeInfo, "valueType", "MethodAmbiguityException: " + e.getMessage());
                System.err.println("MethodAmbiguityException for value type in " + resolved.getName() + ": " + e.getMessage() + " For node :" + nodeInfo);
            } catch (UnsolvedSymbolException exception) {
                registerNodeData(symbolNodeInfo, "valueType", "UnsolvedSymbolException: " + exception.getMessage());
                System.err.println("UnsolvedSymbolException for value type in " + resolved.getName() + ": " + exception.getMessage() + " For node :" + nodeInfo);
            }
        }
    }

    public void describeTypeSymbol(ResolvedTypeNodeInfo typeNodeInfo) {
        var type = typeNodeInfo.getType();
        registerNodeData(typeNodeInfo, "registered_as", "resolved_type");

        registerNodeData(typeNodeInfo, "typeName", type.describe());
//        if (typeNodeInfo is PrimitiveTypeUsage pr)
        registerNodeData(typeNodeInfo, "isReferenceType", type.isReferenceType());
        registerNodeData(typeNodeInfo, "isNull", type.isNull());
        registerNodeData(typeNodeInfo, "isNumericType", type.isNumericType());
        registerNodeData(typeNodeInfo, "isPrimitive", type.isPrimitive());
        registerNodeData(typeNodeInfo, "isReference", type.isReference());
        registerNodeData(typeNodeInfo, "isReferenceType", type.isReferenceType());
        if (type.isReferenceType()) {
            var refType = type.asReferenceType();
            var relationship = new GraphRelationship(typeNodeInfo, registerTypeSymbol(refType), "type_of");
            registerRelationshipData(relationship, "uses", "reference_type");
            var typeDeclaration = refType.getTypeDeclaration();
            if (typeDeclaration.isPresent()) {
                var typeDeclarationD = typeDeclaration.get();
                registerNodeData(typeNodeInfo, "typeDeclaration", typeDeclarationD.getName());

            }
            if (type.isReferenceType() && typeDeclaration.isPresent()) {
                registerSymbol(typeDeclaration.get());
            } else {
                registerNodeData(typeNodeInfo, "typeDeclaration", "N/A");
            }
        } else {
            registerNodeData(typeNodeInfo, "typeName", type.describe());
            registerNodeData(typeNodeInfo, "typeSimpleName", type.describe());
        }

        registerNodeData(typeNodeInfo, "typeHash", Integer.toHexString(type.hashCode()));
        registerNodeData(typeNodeInfo, "isReferenceType", type.isReferenceType());

    }

    public void describeSyntaxInfo(SyntaxNodeInfo nodeInfo) {
        Node node = nodeInfo.node;
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

    public void addParentToNode(SyntaxNodeInfo nodeInfo) {
        Node node = nodeInfo.node;
        if (node.hasParentNode()) {
            Node parentNode = node.getParentNode().orElseThrow();
            SyntaxNodeInfo parentNodeInfo = registerSyntaxNode(parentNode);
            GraphRelationship relationship = new GraphRelationship(nodeInfo, parentNodeInfo, "parent_of");
            registerRelationshipData(relationship, "has_parent", true);
        } else {
            System.err.println("Node has no parent: " + node.getClass().getSimpleName());
        }
    }

    // Extracts symbols for the given syntax node using the JavaParser symbol resolver.
    public void extractSymbols(SyntaxNodeInfo nodeInfo) {
        Node node = nodeInfo.node;
        if (node instanceof Resolvable) {
            try {
                ResolvedDeclaration resolved = ((Resolvable<? extends ResolvedDeclaration>) node).resolve();
                registerSymbol(resolved);
            } catch (Exception e) {
//                symbolInfo.put("error", e.getMessage());
            }
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
                if (resolved instanceof ResolvedMethodDeclaration resolvedMethod) {
                    var returnType = resolvedMethod.getReturnType();
                    registerNodeData(symbolNodeInfo, "returnType", returnType.describe());
                    registerTypeSymbol(returnType);
                } else if (resolved instanceof ResolvedValueDeclaration valueDeclaration) {
                    registerNodeData(symbolNodeInfo, "valueType", valueDeclaration.getType().describe());
                    var valueType = valueDeclaration.getType();
                    registerTypeSymbol(valueType);
                } else if (resolved instanceof ResolvedReferenceTypeDeclaration typeDeclaration) {
                    registerNodeData(symbolNodeInfo, "typeDeclaration_typeName", typeDeclaration.getName());
                    registerNodeData(symbolNodeInfo, "typeDeclaration_typeQualifiedName", typeDeclaration.getQualifiedName());
                    registerNodeData(symbolNodeInfo, "typeDeclaration_typeHash", Integer.toHexString(typeDeclaration.hashCode()));
                } else {
                    registerNodeData(symbolNodeInfo, "typeDeclaration_typeName", "N/A");
                }
            } catch (Exception e) {
                symbolDetails.put("error", e.getMessage());
            }
        } else if (node instanceof NameExpr) {
            try {
                ResolvedDeclaration resolved = ((NameExpr) node).resolve();
                registerSymbol(resolved);
                symbolDetails.put("nameExpr", resolved.getName());


            } catch (Exception e) {
                symbolDetails.put("extractDetailedSymbols_error", e.getMessage());
            }
        } else if (node instanceof com.github.javaparser.ast.expr.MethodCallExpr) { // New branch for MethodCallExpr
            try {
                ResolvedDeclaration resolved = ((com.github.javaparser.ast.expr.MethodCallExpr) node).resolve();
                registerSymbol(resolved);
                symbolDetails.put("methodCall", resolved.getName());
            } catch (Exception e) {
                symbolDetails.put("error", e.getMessage());
            }
        }
        return symbolDetails;
    }
}
