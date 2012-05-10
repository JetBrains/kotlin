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
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.jetbrains.jet.cli.common.ExitCode.*;
import org.jetbrains.jet.cli.common.ExitCode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
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
        final K2JVMCompilerArguments arguments = createCompilerArguments();

        configureCompilerArguments(arguments);

        final K2JVMCompiler compiler = createCompiler();

        printCompilerArgumentsIfDebugEnabled(arguments, compiler);

        final ExitCode exitCode = compiler.exec(System.err, arguments);

        switch (exitCode) {
            case COMPILATION_ERROR:
                throw new MojoExecutionException("Compilation error. See log for more details");

            case INTERNAL_ERROR:
                throw new MojoExecutionException("Internal compiler error. See log for more details");
        }
    }

    private void printCompilerArgumentsIfDebugEnabled(K2JVMCompilerArguments arguments, K2JVMCompiler compiler) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Invoking compiler " + compiler + " with arguments:");
            try {
                Field[] fields = arguments.getClass().getFields();
                for (Field f : fields) {
                    Object value = f.get(arguments);
                    if (value != null) {
                        getLog().debug(f.getName() + "=" + value);
                    }
                }
                getLog().debug("End of arguments");
            }
            catch (Exception e) {
                getLog().warn("Failed to print compiler arguments: " + e, e);
            }
        }
    }

    protected K2JVMCompiler createCompiler() {
        return new K2JVMCompiler();
    }

    /**
     * Derived classes can create custom compiler argument implementations
     * such as for KDoc
     */
    protected K2JVMCompilerArguments createCompilerArguments() {
        return new K2JVMCompilerArguments();
    }

    /**
     * Derived classes can register custom plugins or configurations
     */
    protected abstract void configureCompilerArguments(K2JVMCompilerArguments arguments) throws MojoExecutionException;

    protected void configureBaseCompilerArguments(Log log, K2JVMCompilerArguments arguments, String module,
                                                  List<String> sources, List<String> classpath, String output) throws MojoExecutionException {
        // don't include runtime, it should be in maven dependencies
        arguments.mode = "stdlib";

        final ArrayList<String> classpathList = new ArrayList<String>();

        if (module != null) {
            log.info("Compiling Kotlin module " + module);
            arguments.setModule(module);
        }
        else {
            if (sources.size() <= 0)
                throw new MojoExecutionException("No source roots to compile");

            arguments.setSourceDirs(sources);
            log.info("Compiling Kotlin sources from " + arguments.getSourceDirs());

            // TODO: Move it compiler
            classpathList.addAll(sources);
        }

        classpathList.addAll(classpath);

        if (classpathList.remove(output)) {
            log.debug("Removed target directory from compiler classpath (" + output + ")");
        }

//        final String runtime = getRuntimeFromClassPath(classpath);
//        if (runtime != null) {
//            log.debug("Removed Kotlin runtime from compiler classpath (" + runtime + ")");
//            classpathList.remove(runtime);
//        }

        if (classpathList.size() > 0) {
            final String classPathString = Joiner.on(File.pathSeparator).join(classpathList);
            log.info("Classpath: " + classPathString);
            arguments.setClasspath(classPathString);
        }

        log.info("Classes directory is " + output);
        arguments.setOutputDir(output);

        arguments.jdkHeaders = getJdkHeaders().getPath();
        log.debug("Using jdk headers from " + arguments.jdkHeaders);
    }

    // TODO: Make a better runtime detection or get rid of it entirely
    private String getRuntimeFromClassPath(List<String> classpath) {
        for (String item : classpath) {
            final int lastSeparatorIndex = item.lastIndexOf(File.separator);

            if (lastSeparatorIndex < 0)
                continue;

            if (item.startsWith("kotlin-runtime-", lastSeparatorIndex + 1) && item.endsWith(".jar"))
                return item;
        }

        return null;
    }

    private File jdkHeadersPath;

    protected File getJdkHeaders() {
        if (jdkHeadersPath != null)
            return jdkHeadersPath;

        try {
            jdkHeadersPath = extractJdkHeaders();

            if (jdkHeadersPath == null)
                throw new RuntimeException("Can't find kotlin jdk headers in maven plugin resources");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return jdkHeadersPath;
    }

    private File extractJdkHeaders() throws IOException {
        final String kotlin_jdk_headers = "kotlin-jdk-headers.jar";

        final URL jdkHeadersResource = Resources.getResource(kotlin_jdk_headers);
        if (jdkHeadersResource == null)
            return null;

        final File jdkHeadersTempDir = Files.createTempDir();
        jdkHeadersTempDir.deleteOnExit();

        final File jdkHeadersFile = new File(jdkHeadersTempDir, kotlin_jdk_headers);
        Files.copy(Resources.newInputStreamSupplier(jdkHeadersResource), jdkHeadersFile);

        return jdkHeadersFile;
    }
}
