package org.example;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) {
        // Instantiate the parser
        AtomicInteger i = new AtomicInteger();
        JavaSolutionParser parser = new JavaSolutionParser();
        parser.loadSyntaxNodes("/Users/sim/src/simple-java-scanner");
        // Load syntax nodes from the solution path

        var syntaxNodes = parser.getNodesSet();
        Dictionary<INodeInfo, Map<String, Object>> nodes = parser.getNodeInfos();
        // Iterate through the nodes and print their information
        syntaxNodes.forEach(x -> {
            var j = i.addAndGet(1);
            var daat = nodes.get(x);
            System.out.println(j +" Node ID: " + x.getId() + " Node Type: " + x.getNodeType() + " Data: " + daat);
        });
        System.out.println("Number of nodes loaded: " + syntaxNodes.size());
        // Print the loaded nodes
    }
}
