package org.example;

import org.neo4j.driver.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Neo4JExporter implements AutoCloseable {

    // URI examples: "neo4j://localhost", "neo4j+s://xxx.databases.neo4j.io"
    final String dbUri = "bolt://localhost:7687";
    final String dbUser = "";
    final String dbPassword = "";

    private final Driver driver;

    // New sets to track labels
    private final Set<String> nodeLabels = new HashSet<>();
    private final Set<String> relationshipLabels = new HashSet<>();

    // Constructor: establish connection
    public Neo4JExporter() {
        driver = GraphDatabase.driver(dbUri, AuthTokens.basic(dbUser, dbPassword));
    }

    // Export function: upsert node with label from getNodeType() and id property from getId(), plus extra data.
    public void exportNode(INodeInfo node, Map<String, Object> data) {
        if (data == null) {
            data = new HashMap<>();
        }
        nodeLabels.add(node.getNodeType());
        try (Session session = driver.session()) {
            session.run("MERGE (n:" + node.getNodeType() + " {id: $id}) SET n += $data",
                    Values.parameters("id", node.getId(), "data", data));
            // Added logging for node export
//            System.out.println("Exported node with label: " + node.getNodeType() + " and id: " + node.getId());
        } catch (Exception e) {
            System.err.println("Failed to export node with label: " + node.getNodeType() +
                    " and id: " + node.getId() + ". Error: " + e.getMessage() + "\n\n" +
                    "Data " + data + "\n");
            //throw new RuntimeException(e);
        }
    }

    // Export function: upsert relationship between nodes.
    public void exportRelationShip(GraphRelationship relationship, Map<String, Object> data) {
        // Track labels for nodes and relationship
        nodeLabels.add(relationship.from().getNodeType());
        nodeLabels.add(relationship.to().getNodeType());
        relationshipLabels.add(relationship.label());
        try (Session session = driver.session()) {
            try {
                session.run("MATCH (a:" + relationship.from().getNodeType() + " {id: $fromId}), (b:" + relationship.to().getNodeType() + " {id: $toId}) " +
                                "MERGE (a)-[r:" + relationship.label() + "]->(b) SET r += $data",
                        Values.parameters("fromId", relationship.from().getId(), "toId", relationship.to().getId(), "data", data));
                // Added logging for relationship export
                System.out.println("Exported relationship with label: " + relationship.label() +
                        " between nodes " + relationship.from().getId() + " and " + relationship.to().getId());
            } catch (Exception e) {
                System.err.println("Failed to export node with relationship: " + relationship +". Error: " + e.getMessage() + "\n\n" +
                        "Data " + data + "\n");
                //throw new RuntimeException(e);
            }
        }
    }

    // Create indexes on tracked labels.
    public void createIndexes() {
        try (Session session = driver.session()) {
            // Create id based indexes for node labels
            for (String label : nodeLabels) {
                session.run("CREATE INDEX IF NOT EXISTS FOR (n:" + label + ") ON (n.id)");
                // Added logging for node index creation
                System.out.println("Created index on node label: " + label);
            }
            // Create id based indexes for relationship labels (if supported)
            for (String label : relationshipLabels) {
                session.run("CREATE INDEX IF NOT EXISTS FOR ()-[r:" + label + "]-() ON (r.id)");
                // Added logging for relationship index creation
                System.out.println("Created index on relationship label: " + label);
            }
        }
    }

    // Dispose connection when the class is closed.
    @Override
    public void close() {
        driver.close();
    }
}
