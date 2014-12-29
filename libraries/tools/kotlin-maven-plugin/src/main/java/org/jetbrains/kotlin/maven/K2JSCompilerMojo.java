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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.js.K2JSCompiler;
import org.jetbrains.kotlin.utils.LibraryUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts Kotlin to JavaScript code
 *
 * @goal js
 * @phase compile
 * @requiresDependencyResolution compile
 * @noinspection UnusedDeclaration
 */
public class K2JSCompilerMojo extends KotlinCompileMojoBase<K2JSCompilerArguments> {

    /**
     * The output JS file name
     *
     * @required
     * @parameter default-value="${project.build.directory}/js/${project.artifactId}.js"
     */
    private String outputFile;

    /**
     * The output metafile name
     *
     * @parameter default-value="${project.build.directory}/js/${project.artifactId}.meta.js"
     */
    private String metaFile;

        @Override
    protected void configureSpecificCompilerArguments(@NotNull K2JSCompilerArguments arguments) throws MojoExecutionException {
        arguments.outputFile = outputFile;
        arguments.noStdlib = true;
        arguments.metaInfo = metaFile;

        List<String> libraries = getKotlinJavascriptLibraryFiles();
        LOG.info("libraryFiles: " + libraries);
        arguments.libraryFiles = libraries.toArray(new String[0]);
    }

    /**
     * Returns all Kotlin Javascript dependencies that this project has, including transitive ones.
     * @return array of paths to kotlin javascript libraries
     */
    @NotNull
    private List<String> getKotlinJavascriptLibraryFiles() {
        List<String> libraries = new ArrayList<String>();

        for(Artifact artifact : project.getArtifacts()) {
            if (artifact.getScope().equals(Artifact.SCOPE_COMPILE)) {
                File file = artifact.getFile();
                if (LibraryUtils.isKotlinJavascriptLibrary(file)) {
                    libraries.add(file.getAbsolutePath());
                }
                else {
                    LOG.warn("artifact " + artifact + " is not a Kotlin Javascript Library");
                }
            }
        }

        return libraries;
    }

    @NotNull
    @Override
    protected K2JSCompilerArguments createCompilerArguments() {
        return new K2JSCompilerArguments();
    }

    @NotNull
    @Override
    protected K2JSCompiler createCompiler() {
        return new K2JSCompiler();
    }
}
