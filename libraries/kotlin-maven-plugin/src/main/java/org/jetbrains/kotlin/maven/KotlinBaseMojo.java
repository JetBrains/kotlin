package org.jetbrains.kotlin.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jetbrains.jet.cli.CompilerArguments;
import org.jetbrains.jet.cli.KotlinCompiler;

/**
 * Abstract base class for Kotlin maven plugins
 */
public abstract class KotlinBaseMojo extends AbstractMojo {
    private KotlinCompiler compiler = new KotlinCompiler();
    private CompilerArguments arguments;

    /**
     * The source directory to compile
     *
     * @required
     * @parameter default-value="${basedir}/src/main/kotlin"
     */
    private String src;

    /**
     * The output directory for bytecode classes
     *
     * @required
     * @parameter default-value="${project.build.directory}/classes"
     */
    private String outputDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        arguments = createCompilerArguments();

        if (src != null) {
            arguments.setSrc(src);
        }
        if (outputDir != null) {
            arguments.setOutputDir(outputDir);
        }
        configureCompilerArguments(arguments);

        compiler.exec(System.err, arguments);
    }


    /**
     * Derived classes can create custom compiler argument implementations
     * such as for KDoc
     */
    protected CompilerArguments createCompilerArguments() {
        return new CompilerArguments();
    }

    /**
     * Derived classes can register custom plugins or configurations
     */
    protected void configureCompilerArguments(CompilerArguments arguments) {
    }

}
