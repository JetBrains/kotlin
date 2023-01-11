import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

@Mojo(name = "add-custom-sourceRoots", defaultPhase = LifecyclePhase.COMPILE)
public class AddCustomSourceRootsMojo
        extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    public MavenProject project;

    public void execute()
            throws MojoExecutionException {
        File targetGeneratedFolder = new File(project.getBasedir().getAbsolutePath(), "target/gen");
        if (!targetGeneratedFolder.exists()) {
            if (!targetGeneratedFolder.mkdirs()) {
                throw new MojoExecutionException("Failed to create target directory.");
            }
        }
        File generatedKtFile = new File(targetGeneratedFolder, "new.kt");

        try {
            String str = "fun foo(){\n" +
                    "    println(\"2\")\n" +
                    "}";
            BufferedWriter writer = new BufferedWriter(new FileWriter(generatedKtFile));
            writer.write(str);
            writer.close();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        getLog().info("path: " + generatedKtFile.getAbsolutePath());
        project.addCompileSourceRoot(targetGeneratedFolder.getAbsolutePath());
        getLog().info("CompileSourceRoots: " + project.getCompileSourceRoots().toString());
    }
}
