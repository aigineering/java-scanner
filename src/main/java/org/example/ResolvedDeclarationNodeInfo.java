package org.example;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;

public class ResolvedDeclarationNodeInfo implements INodeInfo {
    private ResolvedDeclaration declaration = null;
    private String id = null;
    private String nodeType = null;

    public ResolvedDeclarationNodeInfo(ResolvedDeclaration declaration) {
        this.declaration = declaration;
        this.id = Integer.toHexString(declaration.toAst().get().hashCode());
        this.nodeType = declaration.getClass().getSimpleName();
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
        return declaration.equals(that.declaration);
    }
}

