import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Mojo(name = "add-source-root", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class AddSourceRootMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    public MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        File generatedSourcesDir = new File(project.getBasedir(), "target/generated-sources/custom-plugin");
        File generatedPackageDir = new File(generatedSourcesDir, "sample");
        if (!generatedPackageDir.exists() && !generatedPackageDir.mkdirs()) {
            throw new MojoExecutionException("Failed to create generated sources directory.");
        }

        File generatedKtFile = new File(generatedPackageDir, "GeneratedByPlugin.kt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(generatedKtFile))) {
            writer.write("package sample\n\nclass GeneratedByPlugin\n");
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        project.addCompileSourceRoot(generatedSourcesDir.getAbsolutePath());
    }
}
