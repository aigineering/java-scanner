package org.example;

import com.github.javaparser.ast.Node;

public class SyntaxNodeInfo implements INodeInfo {
    private String id = null;
    private String nodeType = null;

    public SyntaxNodeInfo(Node node) {
        this.node = node;
        this.id = Integer.toHexString(node.hashCode());
        this.nodeType = node.getClass().getSimpleName();
    }

    public Node node;


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
        return node.hashCode();
    }
    @Override
    public  boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SyntaxNodeInfo that = (SyntaxNodeInfo) obj;
        return this.getId().equals(that.getId()) &&
               this.getNodeType().equals(that.getNodeType());
    }
}

