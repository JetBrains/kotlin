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
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;

import java.util.List;

/**
 * Compiles Kotlin test sources
 *
 * @noinspection UnusedDeclaration
 */
@Mojo(name = "test-compile",
        defaultPhase = LifecyclePhase.TEST_COMPILE,
        requiresDependencyResolution = ResolutionScope.TEST
)
public class KotlinTestCompileMojo extends K2JVMCompileMojo {
    /**
     * Flag to allow test compilation to be skipped.
     */
    @Parameter(property = "maven.test.skip", defaultValue = "false")
    private boolean skip;

    // TODO it would be nice to avoid using 2 injected fields for sources
    // but I've not figured out how to have a defaulted parameter value
    // which is also customisable inside an <execution> in a maven pom.xml
    // so for now lets just use 2 fields

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
     * The source directories containing the sources to be compiled for tests.
     */
    @Parameter(defaultValue = "${project.testCompileSourceRoots}", required = true, readonly = true)
    private List<String> defaultSourceDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Test compilation is skipped");
        } else {
            super.execute();
        }
    }

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2JVMCompilerArguments arguments) throws MojoExecutionException {
        module = testModule;
        classpath = testClasspath;
        arguments.friendPaths = new String[] { output };
        output = testOutput;
        super.configureSpecificCompilerArguments(arguments);
    }

    @Override
    protected List<String> getRelatedSourceRoots(MavenProject project) {
        return project.getTestCompileSourceRoots();
    }
}
