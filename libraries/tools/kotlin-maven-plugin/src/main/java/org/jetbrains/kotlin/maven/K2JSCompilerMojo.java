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
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.CompilerArguments;
import org.jetbrains.jet.cli.js.K2JSCompiler;
import org.jetbrains.jet.cli.js.K2JSCompilerArguments;

/**
 * Converts Kotlin to JavaScript code
 *
 * @goal js
 * @phase compile
 * @noinspection UnusedDeclaration
 */
public class K2JSCompilerMojo extends KotlinCompileMojo {
    /**
     * The output JS file name
     *
     * @required
     * @parameter default-value="${project.build.directory}/js/${project.artifactId}.js"
     */
    private String outFile;

    @Override
    protected void configureCompilerArguments(CompilerArguments arguments) throws MojoExecutionException {
        super.configureCompilerArguments(arguments);

        if (arguments instanceof K2JSCompilerArguments) {
            K2JSCompilerArguments k2jsArgs = (K2JSCompilerArguments)arguments;
            k2jsArgs.outputFile = outFile;
            if (sources.size() > 0) {
                // TODO K2JSCompilerArguments should allow more than one path/file
                k2jsArgs.srcdir = sources.get(0);
            }
        }
        getLog().info("Compiling Kotlin src from " + arguments.getSrc() + " to JavaScript at: " + outFile);
    }

    @Override
    protected CompilerArguments createCompilerArguments() {
        return new K2JSCompilerArguments();
    }

    @Override
    protected CLICompiler createCompiler() {
        return new K2JSCompiler();
    }
}
