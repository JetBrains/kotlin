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

import com.intellij.util.ArrayUtil;
import com.sampullara.cli.Args;
import kotlin.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.join;

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

        List<String> classpathList = filterClassPath(classpath);

        if (!classpathList.isEmpty()) {
            String classPathString = join(classpathList, File.pathSeparator);
            getLog().info("Classpath: " + classPathString);
            arguments.classpath = classPathString;
        }

        getLog().info("Classes directory is " + output);
        arguments.destination = output;

        arguments.moduleName = moduleName;
        getLog().info("Module name is " + moduleName);

        try {
            Args.parse(arguments, ArrayUtil.toStringArray(args));
        }
        catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        if (arguments.noOptimize) {
            getLog().info("Optimization is turned off");
        }
    }

    protected List<String> filterClassPath(List<String> classpath) {
        return CollectionsKt.filter(classpath, new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String s) {
                return new File(s).exists() || new File(project.getBasedir(), s).exists();
            }
        });
    }
}
