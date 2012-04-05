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
import org.jetbrains.jet.cli.CompilerArguments;

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

        K2JSCompilerPlugin plugin = new K2JSCompilerPlugin();
        plugin.setOutFile(outFile);
        arguments.getCompilerPlugins().add(plugin);

        getLog().info("Compiling Kotlin src from " + arguments.getSrc() + " to JavaScript at: " + outFile);
    }
}
