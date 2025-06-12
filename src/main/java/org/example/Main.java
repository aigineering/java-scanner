package org.example;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        // Instantiate the parser
        JavaSolutionParser parser = new JavaSolutionParser();
        // Load syntax nodes from the solution path
        List<?> nodes = parser.loadSyntaxNodes("/Users/sim/src/simple-java-scanner");
        
        // Collect and print output from symbol resolvers for each node
        for (Object nodeObj : nodes) {
            // Cast nodeObj to com.github.javaparser.ast.Node if needed
            com.github.javaparser.ast.Node node = (com.github.javaparser.ast.Node) nodeObj;
            Map<String, Object> symbolDetails = parser.extractDetailedSymbols(node);
            System.out.println(symbolDetails);
        }
    }
}
