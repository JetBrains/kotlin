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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;

import java.io.File;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.join;
import static org.jetbrains.kotlin.maven.Util.filterClassPath;

/**
 * Compiles kotlin sources
 *
 * @noinspection UnusedDeclaration
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class K2JVMCompileMojo extends KotlinCompileMojoBase<K2JVMCompilerArguments> {
    /**
     * Project classpath.
     */
    @Parameter(defaultValue = "${project.compileClasspathElements}", required = true, readonly = true)
    public List<String> classpath;

    /**
     * Project test classpath.
     */
    @Parameter(defaultValue = "${project.testClasspathElements}", required = true, readonly = true)
    protected List<String> testClasspath;

    @Parameter(defaultValue = "${project.artifactId}", required = true, readonly = true)
    protected String moduleName;

    @Parameter(defaultValue = "${project.artifactId}-test", required = true, readonly = true)
    protected String testModuleName;

    @Parameter(property = "kotlin.compiler.jvmTarget", required = false, readonly = false)
    protected String jvmTarget;

    @Parameter(property = "kotlin.compiler.jdkHome", required = false, readonly = false)
    protected String jdkHome;

    @Parameter(property = "kotlin.compiler.scriptTemplates", required = false, readonly = false)
    protected List<String> scriptTemplates;

    @Override
    protected List<String> getRelatedSourceRoots(MavenProject project) {
        return project.getCompileSourceRoots();
    }

    @NotNull
    @Override
    protected K2JVMCompiler createCompiler() {
        return new K2JVMCompiler();
    }

    @NotNull
    @Override
    protected K2JVMCompilerArguments createCompilerArguments() {
        return new K2JVMCompilerArguments();
    }

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2JVMCompilerArguments arguments) throws MojoExecutionException {
        arguments.destination = output;

        // don't include runtime, it should be in maven dependencies
        arguments.noStdlib = true;

        if (module != null) {
            getLog().info("Compiling Kotlin module " + module);
            arguments.module = module;
        }

        List<String> classpathList = filterClassPath(project.getBasedir(), classpath);

        if (!classpathList.isEmpty()) {
            String classPathString = join(classpathList, File.pathSeparator);
            getLog().debug("Classpath: " + classPathString);
            arguments.classpath = classPathString;
        }

        getLog().debug("Classes directory is " + output);
        arguments.destination = output;

        arguments.moduleName = moduleName;
        getLog().info("Module name is " + moduleName);

        if (arguments.noOptimize) {
            getLog().info("Optimization is turned off");
        }

        arguments.jvmTarget = jvmTarget;

        if (jdkHome != null) {
            getLog().info("Overriding JDK home path with: " + jdkHome);
            arguments.jdkHome = jdkHome;
        }

        if (scriptTemplates != null && !scriptTemplates.isEmpty()) {
            arguments.scriptTemplates = scriptTemplates.toArray(new String[0]);
        }
    }
}
