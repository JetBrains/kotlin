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
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments;

import java.util.List;

/**
 * Compiles Kotlin test sources
 *
 * @goal test-compile
 * @phase test-compile
 * @requiresDependencyResolution test
 * @noinspection UnusedDeclaration
 */
public class KotlinTestCompileMojo extends KotlinCompileMojoBase {
    /**
     * Flag to allow test compilation to be skipped.
     *
     * @parameter expression="${maven.test.skip}" default-value="false"
     * @noinspection UnusedDeclaration
     */
    private boolean skip;

    // TODO it would be nice to avoid using 2 injected fields for sources
    // but I've not figured out how to have a defaulted parameter value
    // which is also customisable inside an <execution> in a maven pom.xml
    // so for now lets just use 2 fields

    /**
     * The default source directories containing the sources to be compiled.
     *
     * @parameter default-value="${project.testCompileSourceRoots}"
     * @required
     */
    private List<String> defaultSourceDirs;

    /**
     * The source directories containing the sources to be compiled.
     *
     * @parameter
     */
    private List<String> sourceDirs;

    @Override
    public List<String> getSources() {
        if (sourceDirs != null && !sourceDirs.isEmpty()) return sourceDirs;
        return defaultSourceDirs;
    }

    /**
     * The source directories containing the sources to be compiled for tests.
     *
     * @parameter default-value="${project.testCompileSourceRoots}"
     * @required
     * @readonly
     */
    private List<String> defaultSourceDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Test compilation is skipped");
        }
        else {
            super.execute();
        }
    }

    @Override
    protected void configureCompilerArguments(CommonCompilerArguments arguments) throws MojoExecutionException {
        if (arguments instanceof K2JVMCompilerArguments) {
            configureBaseCompilerArguments(
                    getLog(), (K2JVMCompilerArguments) arguments,
                    testModule, getSources(), testClasspath, testOutput);
        }
    }
}
