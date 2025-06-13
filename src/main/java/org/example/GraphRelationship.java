package org.example;

import java.util.Dictionary;

public record GraphRelationship (INodeInfo from, INodeInfo to, String label) {}
