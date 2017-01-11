package org.jetbrains.kotlin.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments;

import java.io.File;
import java.util.List;

@Mojo(name = "test-metadata", defaultPhase = LifecyclePhase.PROCESS_TEST_SOURCES, requiresDependencyResolution = ResolutionScope.TEST)
public class TestMetadataMojo extends MetadataMojo {
    @Parameter(property = "maven.test.skip", defaultValue = "false")
    private boolean skip;

    @Override
    protected List<String> getSourceFilePaths() {
        return project.getTestCompileSourceRoots();
    }

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2MetadataCompilerArguments arguments) throws MojoExecutionException {
        String productionOutput = output;

        module = testModule;
        classpath = testClasspath;
//        arguments.friendPaths = new String[] { productionOutput };
        output = testOutput;
        super.configureSpecificCompilerArguments(arguments);

        if (arguments.classpath == null) {
            arguments.classpath = productionOutput;
        } else {
            arguments.classpath = arguments.classpath + File.pathSeparator + productionOutput;
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Test compilation is skipped");
        } else {
            super.execute();
        }
    }
}
