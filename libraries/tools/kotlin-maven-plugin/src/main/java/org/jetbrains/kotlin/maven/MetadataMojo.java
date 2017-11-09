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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments;
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.join;
import static org.jetbrains.kotlin.maven.Util.filterClassPath;

@Mojo(name = "metadata", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class MetadataMojo extends KotlinCompileMojoBase<K2MetadataCompilerArguments> {
    @Parameter(defaultValue = "${project.compileClasspathElements}", required = true, readonly = true)
    public List<String> classpath;

    @Parameter(defaultValue = "${project.testClasspathElements}", required = true, readonly = true)
    public List<String> testClasspath;

    @Override
    protected List<String> getRelatedSourceRoots(MavenProject project) {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    protected CLICompiler<K2MetadataCompilerArguments> createCompiler() {
        return new K2MetadataCompiler();
    }

    @NotNull
    @Override
    protected K2MetadataCompilerArguments createCompilerArguments() {
        return new K2MetadataCompilerArguments();
    }

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2MetadataCompilerArguments arguments, @NotNull List<File> sourceRoots) throws MojoExecutionException {
        arguments.setDestination(output);
        if (!arguments.getMultiPlatform()) {
            getLog().info("multiPlatform forced for metadata generation");
            arguments.setMultiPlatform(true);
        }

        List<String> classpathList = filterClassPath(project.getBasedir(), classpath);
        classpathList.remove(project.getBuild().getOutputDirectory());

        if (!classpathList.isEmpty()) {
            String classPathString = join(classpathList, File.pathSeparator);
            getLog().debug("Classpath: " + classPathString);
            arguments.setClasspath(classPathString);
        }
    }
}
