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

@Mojo(name = "add-many-source-files", defaultPhase = LifecyclePhase.COMPILE)
public class AddManySourceFilesMojo
        extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    public MavenProject project;

    public void execute()
            throws MojoExecutionException {
        File srcFolder = new File(project.getBasedir().getAbsolutePath(), "src/main/kotlin");
        if (!srcFolder.exists()) {
            throw new MojoExecutionException("Src dir is not present");
        }
        int numberOfFiles = 500;
        for (Integer count = 0; count < numberOfFiles; count++) {
            File generatedKtFile = new File(srcFolder, "SmallFileWithBigNameXXXXXXXXXXXXXXXXXXXXXXXXXX" + count.toString() + ".kt");
            try {
                String str = "fun foo" + count.toString() + "(){\n" +
                        "    println(\"2\")\n" +
                        "}";
                BufferedWriter writer = new BufferedWriter(new FileWriter(generatedKtFile));
                writer.write(str);
                writer.close();
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
    }
}
