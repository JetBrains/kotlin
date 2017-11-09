/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments;

import java.io.File;
import java.util.List;

@Mojo(name = "test-metadata", defaultPhase = LifecyclePhase.PROCESS_TEST_SOURCES, requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true
)
public class TestMetadataMojo extends MetadataMojo {
    @Parameter(property = "maven.test.skip", defaultValue = "false")
    private boolean skip;

    @Override
    protected List<String> getSourceFilePaths() {
        return project.getTestCompileSourceRoots();
    }

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2MetadataCompilerArguments arguments, @NotNull List<File> sourceRoots) throws MojoExecutionException {
        String productionOutput = output;

        classpath = testClasspath;
//        arguments.friendPaths = new String[] { productionOutput };
        output = testOutput;
        super.configureSpecificCompilerArguments(arguments, sourceRoots);

        if (arguments.getClasspath() == null) {
            arguments.setClasspath(productionOutput);
        } else {
            arguments.setClasspath(arguments.getClasspath() + File.pathSeparator + productionOutput);
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
