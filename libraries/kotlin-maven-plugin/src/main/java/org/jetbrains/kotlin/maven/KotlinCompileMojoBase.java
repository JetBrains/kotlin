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

import com.google.common.base.Joiner;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.jet.cli.CompilerArguments;
import org.jetbrains.jet.cli.KotlinCompiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class KotlinCompileMojoBase extends AbstractMojo {
    /**
     * The source directories containing the sources to be compiled.
     *
     * @parameter default-value="${project.compileSourceRoots}"
     * @required
     * @readonly
     */
    public List<String> sources;

    /**
     * The source directories containing the sources to be compiled for tests.
     *
     * @parameter default-value="${project.testCompileSourceRoots}"
     * @required
     * @readonly
     */
    protected List<String> testSources;

    /**
     * Project classpath.
     *
     * @parameter default-value="${project.compileClasspathElements}"
     * @required
     * @readonly
     */
    public List<String> classpath;

    /**
     * Project test classpath.
     *
     * @parameter default-value="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    protected List<String> testClasspath;

    /**
     * The directory for compiled classes.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    public String output;

    /**
     * The directory for compiled tests classes.
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     * @required
     * @readonly
     */
    public String testOutput;

    /**
     * Kotlin compilation module, as alternative to source files or folders.
     *
     * @parameter
     */
    public String module;

    /**
     * Kotlin compilation module, as alternative to source files or folders (for tests).
     *
     * @parameter
     */
    public String testModule;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final CompilerArguments arguments = createCompilerArguments();

        configureCompilerArguments(arguments);

        final KotlinCompiler compiler = createCompiler();

        final KotlinCompiler.ExitCode exitCode = compiler.exec(System.err, arguments);

        switch (exitCode) {
            case COMPILATION_ERROR:
                throw new MojoExecutionException("Compilation error. See log for more details");

            case INTERNAL_ERROR:
                throw new MojoExecutionException("Internal compiler error. See log for more details");
        }
    }

    protected KotlinCompiler createCompiler() {
        return new KotlinCompiler();
    }

    /**
     * Derived classes can create custom compiler argument implementations
     * such as for KDoc
     */
    protected CompilerArguments createCompilerArguments() {
        return new CompilerArguments();
    }

    /**
     * Derived classes can register custom plugins or configurations
     */
    protected abstract void configureCompilerArguments(CompilerArguments arguments) throws MojoExecutionException;

    protected static void configureBaseCompilerArguments(Log log, CompilerArguments arguments, String module,
                                                         List<String> sources, List<String> classpath, String output) throws MojoExecutionException {
        if (module != null) {
            log.info("Compiling Kotlin module " + module);
            arguments.setModule(module);
        } else {
            if (sources.size() <= 0)
                throw new MojoExecutionException("No source roots to compile");

            if (sources.size() > 1)
                throw new MojoExecutionException("Multiple source roots are not supported yet");

            final String src = sources.get(0);

            log.info("Compiling Kotlin sources from " + src);
            arguments.setSrc(src);
        }

        final ArrayList<String> classpathList = new ArrayList<String>(classpath);

        if (classpathList.remove(output)) {
            log.debug("Removed target directory from classpath (" + output + ")");
        }

        final String classPathString = Joiner.on(File.pathSeparator).join(classpathList);
        log.info("Classpath: " + classPathString);
        arguments.setClasspath(classPathString);

        log.info("Classes directory is " + output);
        arguments.setOutputDir(output);
    }
}
