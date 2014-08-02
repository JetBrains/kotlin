package org.jetbrains.kotlin.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.k2js.config.ClassPathLibraryDefintionsConfig;
import org.jetbrains.k2js.config.ClassPathLibrarySourcesLoader;

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
     * The Kotlin JavaScript library source code; the library code to be compiled to JavaScript
     *
     * @parameter expression="${jsLibrarySourceDir}"
     */
    private File librarySourceDir;

    /**
     * The Kotlin JavaScript definition source code; the kotlin code used to define APIs available in the kotlin-lib.js file
     *
     * @parameter expression="${jsDefinitionSourceDir}"
     */
    private File definitionSourceDir;

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
        if (librarySourceDir != null) {
            if (!librarySourceDir.exists()) {
                getLog().warn("Source directory does not exist: " + librarySourceDir);
            } else {
                try {
                    File metaInfFile = new File(outputDir, ClassPathLibrarySourcesLoader.META_INF_SERVICES_FILE);
                    metaInfFile.getParentFile().mkdirs();

                    FileUtils.copyDirectoryStructure(librarySourceDir, outputDir);

                    // now lets generate the META-INF/services/org.jetbrains.kotlin.js.librarySource file
                    PrintWriter writer = new PrintWriter(new FileWriter(metaInfFile));
                    appendSourceFiles(writer, outputDir.getCanonicalPath(), outputDir);
                    writer.close();
                } catch (IOException e) {
                    throw new MojoFailureException(e.getMessage(), e);
                }
            }
        }
        if (definitionSourceDir != null) {
            if (!definitionSourceDir.exists()) {
                getLog().warn("Definition directory does not exist: " + definitionSourceDir);
            } else {
                try {
                    File metaInfFile = new File(outputDir, ClassPathLibraryDefintionsConfig.META_INF_SERVICES_FILE);
                    metaInfFile.getParentFile().mkdirs();

                    FileUtils.copyDirectoryStructure(definitionSourceDir, outputDir);

                    // now lets generate the META-INF/services/org.jetbrains.kotlin.js.libraryDefinitions file
                    PrintWriter writer = new PrintWriter(new FileWriter(metaInfFile));
                    appendSourceFiles(writer, definitionSourceDir.getCanonicalPath(), definitionSourceDir);
                    writer.close();
                } catch (IOException e) {
                    throw new MojoFailureException(e.getMessage(), e);
                }
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
