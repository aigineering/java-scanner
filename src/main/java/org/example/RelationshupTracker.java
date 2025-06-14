package org.example;

import java.util.HashSet;

public class RelationshupTracker {
    private HashSet<GraphRelationship> allRelationships = new HashSet<>();
    private HashSet<GraphRelationship> newRelationships = new HashSet<>();

    public boolean registerRelationship(GraphRelationship relationship) {
        if (allRelationships.contains(relationship)) {
            return false; // Relationship already exists
        }
        allRelationships.add(relationship);
        newRelationships.add(relationship);
        return true; // Relationship added successfully
    }

    public void clearNewRelationships() {
        newRelationships.clear();
    }

    public HashSet<GraphRelationship> getNewRelationships() {
        return new HashSet<>(newRelationships); // Return a copy to avoid external modification
    }

    public HashSet<GraphRelationship> getAllRelationships() {
        return new HashSet<>(allRelationships); // Return a copy to avoid external modification
    }
}
