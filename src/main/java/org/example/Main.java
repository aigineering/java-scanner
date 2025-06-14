package org.example;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws XmlPullParserException, IOException {
        // Instantiate the parser
        AtomicInteger i = new AtomicInteger();
        JavaSolutionParser parser = new JavaSolutionParser("/Users/sim/src/simple-java-scanner");
        parser.loadSyntaxNodes();
        parser.enrichAndDiscoverMoreNodes();
        // Load syntax nodes from the solution path

        var syntaxNodes = parser.getNodesSet();
        Dictionary<INodeInfo, Map<String, Object>> nodes = parser.getNodeInfos();
        var relationshipMapMap = parser.relationshipData();
        var relationships = parser.getRelationships();
        // Iterate through the nodes and print their information

        System.out.println("Number of nodes loaded: " + syntaxNodes.size());
        // Print the loaded nodes

        // Export nodes into Neo4j
        try (Neo4JExporter exporter = new Neo4JExporter()) {
            syntaxNodes.forEach(x -> {
                exporter.exportNode(x, nodes.get(x));
            });
            relationships.forEach(x -> {
                exporter.exportRelationShip(x, relationshipMapMap.get(x));
            });
        }
    }
}
