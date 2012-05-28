package org.jetbrains.kotlin.maven;

import com.google.common.io.Files;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Copies Kotlin source into a JAR to be used when compiling Kotlin to JavaScript
 *
 * @goal jslib
 * @phase compile
 */
public class JSSourceJarMojo extends AbstractMojo {
    /**
     * The Kotlin JavaScript library source code
     *
     * @required
     * @parameter expression="${jsLibrarySourceDir}"
     */
    private File librarySourceDir;

    /**
     * The directory for the copied code
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    public File outputDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!librarySourceDir.exists()) {
            getLog().warn("Source directory does not exist: " + librarySourceDir);
        } else {
            try {
                File metaInfFile = new File(outputDir, "META-INF/services/org.jetbrains.kotlin.js.librarySource");
                metaInfFile.getParentFile().mkdirs();

                FileUtils.copyDirectoryStructure(librarySourceDir, outputDir);

                // lets copy the standard JS library source too
                FileUtils.copyFile(new File(librarySourceDir, "../../js.translator/testFiles/kotlin_lib.js"), new File(outputDir, "kotlin-lib.js"));

                // now lets generate the META-INF/services/org.jetbrains.kotlin.js.librarySource file
                PrintWriter writer = new PrintWriter(new FileWriter(metaInfFile));
                appendSourceFiles(writer, outputDir.getCanonicalPath(), outputDir);
                writer.close();
            } catch (IOException e) {
                throw new MojoFailureException(e.getMessage(), e);
            }
        }
    }

    private void appendSourceFiles(PrintWriter writer, String rootPath, File currentDir) throws IOException {
        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    appendSourceFiles(writer, rootPath, file);
                } else {
                    String name = file.getName();
                    if (name.toLowerCase().endsWith(".kt")) {
                         // lets get the canonical name after removing the root dir
                        String fullPath = file.getCanonicalPath();
                        if (fullPath.startsWith(rootPath)) {
                            String relativePath = fullPath.substring(rootPath.length());
                            if (relativePath.startsWith("/") || relativePath.startsWith(File.separator)) {
                                relativePath = relativePath.substring(1).replace(File.separatorChar, '/');
                            }
                            writer.println(relativePath);
                        } else {
                            getLog().warn("Could not remove the root path " + rootPath + " from file: " + fullPath);
                        }
                    }
                }
            }
        }
    }
}
