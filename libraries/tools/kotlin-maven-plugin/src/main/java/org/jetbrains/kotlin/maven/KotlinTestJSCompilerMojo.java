/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;

import java.util.List;

/**
 * Converts Kotlin to JavaScript code
 *
 * @noinspection UnusedDeclaration
 */
@Mojo(name = "test-js",
        defaultPhase = LifecyclePhase.TEST_COMPILE,
        requiresDependencyResolution = ResolutionScope.TEST
)
public class KotlinTestJSCompilerMojo extends K2JSCompilerMojo {

    /**
     * Flag to allow test compilation to be skipped.
     */
    @Parameter(property = "maven.test.skip", defaultValue = "false")
    private boolean skip;

    /**
     * The default source directories containing the sources to be compiled.
     */
    @Parameter(defaultValue = "${project.testCompileSourceRoots}", required = true)
    private List<String> defaultSourceDirs;

    /**
     * The source directories containing the sources to be compiled.
     */
    @Parameter
    private List<String> sourceDirs;

    @Override
    public List<String> getSourceFilePaths() {
        if (sourceDirs != null && !sourceDirs.isEmpty()) return sourceDirs;
        return defaultSourceDirs;
    }

    /**
     * The output JS file name
     */
    @Parameter(defaultValue = "${project.build.directory}/test-js/${project.artifactId}-tests.js", required = true)
    private String outputFile;

    /**
     * Flag enables or disables metafile generation
     */
    @Parameter(defaultValue = "true")
    private boolean metaInfo;

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2JSCompilerArguments arguments) throws MojoExecutionException {
        module = testModule;
        output = testOutput;

        super.configureSpecificCompilerArguments(arguments);

        arguments.outputFile = outputFile;
        arguments.metaInfo = metaInfo;
    }

    @Override
    protected List<String> getRelatedSourceRoots(MavenProject project) {
        return project.getTestCompileSourceRoots();
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
