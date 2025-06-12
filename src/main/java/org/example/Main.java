package org.example;

import java.util.Dictionary;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Instantiate the parser
        JavaSolutionParser parser = new JavaSolutionParser();
        parser.loadSyntaxNodes("/Users/sim/src/simple-java-scanner");
        // Load syntax nodes from the solution path

        var syntaxNodes = parser.getNodesSet();
        Dictionary<INodeInfo, Map<String, Object>> nodes = parser.getNodeInfos();
        // Iterate through the nodes and print their information
        syntaxNodes.forEach(x -> {
            var daat = nodes.get(x);
            System.out.println("Node ID: " + x.getId() + " Node Type: " + x.getNodeType() + " Data: " + daat);
        });

        // Print the loaded nodes
    }
}
