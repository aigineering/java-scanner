package org.example;

public class ResolvedTypeNodeInfo implements INodeInfo {
    private com.github.javaparser.resolution.types.ResolvedType type = null;
    private String id = null;
    private String nodeType = null;

    public ResolvedTypeNodeInfo(com.github.javaparser.resolution.types.ResolvedType type) {
        this.type = type;
        this.id = Integer.toHexString(type.hashCode());
        this.nodeType = type.getClass().getSimpleName();
    }

    public com.github.javaparser.resolution.types.ResolvedType getType() {
        return type;
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
        return type.hashCode();
    }

    @Override
    public  boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ResolvedTypeNodeInfo that = (ResolvedTypeNodeInfo) obj;
        return this.getId().equals(that.getId()) &&
                this.getNodeType().equals(that.getNodeType());
    }
}
