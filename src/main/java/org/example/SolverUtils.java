package org.example;

import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class SolverUtils {

    /**
     * Reads the given pom.xml, locates each declared dependency in the local Maven repo,
     * and registers its JAR with the provided CombinedTypeSolver.
     *
     * @param combinedTypeSolver the solver to register dependency JARs with
     * @param pomPath            filesystem path to the project's pom.xml
     * @throws IOException               if reading the POM or locating a JAR fails
     * @throws XmlPullParserException    if the POM XML is malformed
     */
    public static void registerJarsFromPom(CombinedTypeSolver combinedTypeSolver, String pomPath)
            throws IOException, XmlPullParserException {

        // 1. Parse the POM
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader(pomPath));  // :contentReference[oaicite:6]{index=6}

        // 2. Iterate all dependencies
        List<Dependency> dependencies = model.getDependencies();
        String userRepo = System.getProperty("user.home") + "/.m2/repository";  // :contentReference[oaicite:7]{index=7}

        for (Dependency dep : dependencies) {
            // 3. Build the path to the JAR
            String groupPath = dep.getGroupId().replace('.', File.separatorChar);
            String artifact = dep.getArtifactId();
            String version = dep.getVersion();
            String classifier = dep.getClassifier() != null ? "-" + dep.getClassifier() : "";
            String jarFileName = artifact + "-" + version + classifier + ".jar";

            String jarPath = Paths.get(userRepo, groupPath, artifact, version, jarFileName)
                    .toAbsolutePath()
                    .toString();

            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                // could add logging or throw if strict
                System.err.println("Warning: JAR not found for " +
                        dep.getGroupId() + ":" + artifact + ":" + version +
                        " at " + jarPath);
                continue;
            }

            // 4. Register with JarTypeSolver
            combinedTypeSolver.add(JarTypeSolver.getJarTypeSolver(jarPath));  // :contentReference[oaicite:8]{index=8}
        }
    }
}
