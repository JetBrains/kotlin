package org.jetbrains.kotlin.maven;

import org.jetbrains.jet.cli.CompilerArguments;

/**
 * Converts Kotlin to JavaScript code
 *
 * @goal js
 * @phase compile
 */
public class K2JSCompilerMojo extends KotlinBaseMojo {

    /**
     * The output JS file name
     *
     * @required
     * @parameter default-value="${project.build.directory}/js/${project.artifactId}.js"
     */
    private String outFile;

    @Override
    protected void configureCompilerArguments(CompilerArguments arguments) {
        super.configureCompilerArguments(arguments);
        K2JSCompilerPlugin plugin = new K2JSCompilerPlugin();
        plugin.setOutFile(outFile);
        arguments.getCompilerPlugins().add(plugin);
        getLog().info("Compiling Kotlin src from " + arguments.getSrc() + " to JavaScript at: " + outFile);
    }

    @Override
    protected CompilerArguments createCompilerArguments() {
        CompilerArguments answer = new CompilerArguments();
        return answer;

    }
}
