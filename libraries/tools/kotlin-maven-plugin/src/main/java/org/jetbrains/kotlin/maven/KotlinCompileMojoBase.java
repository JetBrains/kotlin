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

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.CompilerArguments;
import org.jetbrains.jet.cli.common.CompilerVersion;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.join;

public abstract class KotlinCompileMojoBase extends AbstractMojo {


    // TODO it would be nice to avoid using 2 injected fields for sources
    // but I've not figured out how to have a defaulted parameter value
    // which is also customisable inside an <execution> in a maven pom.xml
    // so for now lets just use 2 fields

    /**
     * The default source directories containing the sources to be compiled.
     *
     * @parameter default-value="${project.compileSourceRoots}"
     * @required
     */
    private List<String> defaultSourceDirs;

    /**
     * The source directories containing the sources to be compiled.
     *
     * @parameter
     */
    private List<String> sourceDirs;

    public List<String> getSources() {
        if (sourceDirs != null && !sourceDirs.isEmpty()) return sourceDirs;
        return defaultSourceDirs;
    }

    /**
     * The directories used to scan for annotation.xml files for Kotlin annotations
     *
     * @parameter
     */
    public List<String> annotationPaths;

    // TODO not sure why this doesn't work :(
    // * @parameter default-value="$(project.basedir}/src/main/resources"



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
        getLog().info("Kotlin Compiler version " + CompilerVersion.VERSION);

        // Check sources
        List<String> sources = getSources();
        if (sources != null && sources.size() > 0) {
            boolean sourcesExists = false;

            for (String source : sources) {
                if (new File(source).exists()) {
                    sourcesExists = true;
                    break;
                }
            }

            if (!sourcesExists) {
                getLog().warn( "No sources found skipping Kotlin compile" );
                return;
            }
        }

        final CompilerArguments arguments = createCompilerArguments();
        configureCompilerArguments(arguments);

        final CLICompiler compiler = createCompiler();
        printCompilerArgumentsIfDebugEnabled(arguments, compiler);

        final Log log = getLog();
        MessageCollector messageCollector = new MessageCollector() {
            @Override
            public void report(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location) {
                String path = location.getPath();
                String position = path == null ? "" : path + ": (" + (location.getLine() + ", " + location.getColumn()) + ") ";

                String text = position + message;

                if (CompilerMessageSeverity.VERBOSE.contains(severity)) {
                    log.debug(text);
                } else if (CompilerMessageSeverity.ERRORS.contains(severity)) {
                    log.error(text);
                } else if (severity == CompilerMessageSeverity.INFO) {
                    log.info(text);
                } else {
                    log.warn(text);
                }
            }
        };

        final ExitCode exitCode = compiler.exec(messageCollector, arguments);

        switch (exitCode) {
            case COMPILATION_ERROR:
                throw new MojoExecutionException("Compilation error. See log for more details");

            case INTERNAL_ERROR:
                throw new MojoExecutionException("Internal compiler error. See log for more details");
        }
    }

    private void printCompilerArgumentsIfDebugEnabled(CompilerArguments arguments, CLICompiler compiler) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Invoking compiler " + compiler + " with arguments:");
            try {
                Field[] fields = arguments.getClass().getFields();
                for (Field f : fields) {
                    Object value = f.get(arguments);

                    String valueString;
                    if (value instanceof Object[]) {
                        valueString = Arrays.deepToString((Object[]) value);
                    }
                    else if (value != null) {
                        valueString = String.valueOf(value);
                    }
                    else {
                        valueString = "(null)";
                    }

                    getLog().debug(f.getName() + "=" + valueString);
                }
                getLog().debug("End of arguments");
            }
            catch (Exception e) {
                getLog().warn("Failed to print compiler arguments: " + e, e);
            }
        }
    }

    protected CLICompiler createCompiler() {
        return new K2JVMCompiler();
    }

    /**
     * Derived classes can create custom compiler argument implementations
     * such as for KDoc
     */
    protected CompilerArguments createCompilerArguments() {
        return new K2JVMCompilerArguments();
    }

    /**
     * Derived classes can register custom plugins or configurations
     */
    protected abstract void configureCompilerArguments(CompilerArguments arguments) throws MojoExecutionException;

    protected void configureBaseCompilerArguments(Log log, K2JVMCompilerArguments arguments, String module,
                                                  List<String> sources, List<String> classpath, String output) throws MojoExecutionException {
        // don't include runtime, it should be in maven dependencies
        arguments.noStdlib = true;

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

        if (classpathList.size() > 0) {
            final String classPathString = Joiner.on(File.pathSeparator).join(classpathList);
            log.info("Classpath: " + classPathString);
            arguments.setClasspath(classPathString);
        }

        log.info("Classes directory is " + output);
        arguments.setOutputDir(output);

        arguments.noJdkAnnotations = true;
        arguments.annotations = getFullAnnotationsPath(log, annotationPaths);
        log.info("Using kotlin annotations from " + arguments.annotations);
    }

    protected String getFullAnnotationsPath(Log log, List<String> annotations) {
        String jdkAnnotation = getJdkAnnotations().getPath();

        List<String> list = new ArrayList<String>();
        if (jdkAnnotation != null && jdkAnnotation.length() > 0) {
            list.add(jdkAnnotation);
        }
        if (annotations != null) {
            for (String annotationPath : annotations) {
                if (new File(annotationPath).exists()) {
                    list.add(annotationPath);
                } else {
                    log.info("annotation path " + annotationPath + " does not exist");
                }
            }
        }
        return join(list, File.pathSeparator);
    }


    private File jdkAnnotationsPath;

    protected File getJdkAnnotations() {
        if (jdkAnnotationsPath != null)
            return jdkAnnotationsPath;

        try {
            jdkAnnotationsPath = extractJdkAnnotations();

            if (jdkAnnotationsPath == null)
                throw new RuntimeException("Can't find kotlin jdk annotations in maven plugin resources");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return jdkAnnotationsPath;
    }

    private File extractJdkAnnotations() throws IOException {
        final String kotlin_jdk_annotations = "kotlin-jdk-annotations.jar";

        final URL jdkAnnotationsResource = Resources.getResource(kotlin_jdk_annotations);
        if (jdkAnnotationsResource == null)
            return null;

        final File jdkAnnotationsTempDir = Files.createTempDir();
        jdkAnnotationsTempDir.deleteOnExit();

        final File jdkAnnotationsFile = new File(jdkAnnotationsTempDir, kotlin_jdk_annotations);
        Files.copy(Resources.newInputStreamSupplier(jdkAnnotationsResource), jdkAnnotationsFile);

        return jdkAnnotationsFile;
    }
}
