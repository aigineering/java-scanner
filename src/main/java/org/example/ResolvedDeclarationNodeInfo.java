package org.example;

import com.github.javaparser.resolution.declarations.ResolvedDeclaration;

public class ResolvedDeclarationNodeInfo implements INodeInfo {
    private ResolvedDeclaration declaration = null;
    private String id = null;
    private String nodeType = null;

    private  int size = -100; // Default size, can be adjusted later if needed
    public ResolvedDeclarationNodeInfo(ResolvedDeclaration declaration) {
        this.declaration = declaration;

//        var ast = declaration.toAst();
//
//        if (ast.isEmpty()) {
//            throw new IllegalArgumentException("ResolvedDeclaration must have an AST node.");
//        }

        this.id = Integer.toHexString(declaration.hashCode());
        try {
            this.nodeType = declaration.getClass().getSimpleName();
            if (this.nodeType.isEmpty()) {
                this.nodeType = "UnknownDeclaration";
            }
        } catch (Exception e) {
            this.nodeType = "UnknownDeclaration";
        }
    }

    public ResolvedDeclaration getDeclaration() {
        return declaration;
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getNodeType() {
        return nodeType;
    }

    @Override
    public int hashCode() {
        return declaration.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ResolvedDeclarationNodeInfo that = (ResolvedDeclarationNodeInfo) obj;
        return this.getId().equals(that.getId()) &&
                this.getNodeType().equals(that.getNodeType());
    }
}

