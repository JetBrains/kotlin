/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.cli.common.CompilerArguments;
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments;

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

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Test compilation is skipped");
        }
        else {
            super.execute();
        }
    }

    @Override
    protected void configureCompilerArguments(CompilerArguments arguments) throws MojoExecutionException {
        if (arguments instanceof K2JVMCompilerArguments) {
            configureBaseCompilerArguments(
                    getLog(), (K2JVMCompilerArguments) arguments,
                    testModule, testSources, testClasspath, testOutput);
        }
    }
}
