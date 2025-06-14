package org.example;

import java.util.HashSet;

public class NodeInfoTracker {
    private HashSet<INodeInfo> allNodes = new HashSet<>();
    private HashSet<INodeInfo> newNodes = new HashSet<>();
    public boolean registerNode(INodeInfo node) {
        if (allNodes.contains(node)) {
            return false; // Node already exists
        }
        allNodes.add(node);
        newNodes.add(node);
        return true; // Node added successfully
    }
    public void clearNewNodes() {
        newNodes.clear();
    }
    public HashSet<INodeInfo> getNewNodes() {
        return new HashSet<>(newNodes); // Return a copy to avoid external modification
    }
    public HashSet<INodeInfo> getAllNodes() {
        return new HashSet<>(allNodes); // Return a copy to avoid external modification
    }

}
