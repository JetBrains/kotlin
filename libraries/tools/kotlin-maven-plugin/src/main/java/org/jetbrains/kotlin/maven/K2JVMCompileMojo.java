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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunnerKt;
import org.jetbrains.kotlin.maven.incremental.MavenICReporter;
import org.jetbrains.kotlin.maven.kapt.AnnotationProcessingManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static com.intellij.openapi.util.text.StringUtil.join;
import static org.jetbrains.kotlin.maven.Util.filterClassPath;

/**
 * Compiles kotlin sources
 *
 * @noinspection UnusedDeclaration
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
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

    @Parameter(property = "kotlin.compiler.incremental", defaultValue = "false", required = false, readonly = false)
    private boolean myIncremental;

    @Parameter(property = "kotlin.compiler.incremental.cache.root", defaultValue = "${project.build.directory}/kotlin-ic", required = false, readonly = false)
    public String incrementalCachesRoot;

    @Parameter(property = "kotlin.compiler.javaParameters", required = false, readonly = false)
    protected boolean javaParameters;

    @NotNull
    private File getCachesDir() {
        return new File(incrementalCachesRoot, getSourceSetName());
    }

    protected boolean isIncremental() {
        return myIncremental;
    }

    private boolean isIncrementalSystemProperty() {
        String value = System.getProperty("kotlin.incremental");
        return value != null && value.equals("true");
    }

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
    protected List<String> getSourceFilePaths() {
        List<String> paths = super.getSourceFilePaths();

        File sourcesDir = AnnotationProcessingManager.getGeneratedSourcesDirectory(project, getSourceSetName());
        if (sourcesDir.isDirectory()) {
            paths = new ArrayList<String>(paths);
            paths.add(sourcesDir.getAbsolutePath());
        }

        return paths;
    }

    @NotNull
    protected String getSourceSetName() {
        return AnnotationProcessingManager.COMPILE_SOURCE_SET_NAME;
    }

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2JVMCompilerArguments arguments) throws MojoExecutionException {
        arguments.destination = output;

        // don't include runtime, it should be in maven dependencies
        arguments.noStdlib = true;
        arguments.javaParameters = this.javaParameters;

        //noinspection deprecation
        if (module != null || testModule != null) {
            getLog().warn("Parameters module and testModule are deprecated and ignored, they will be removed in further release.");
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

        if (jvmTarget != null) {
            arguments.jvmTarget = jvmTarget;
        }

        if (jdkHome != null) {
            getLog().info("Overriding JDK home path with: " + jdkHome);
            arguments.jdkHome = jdkHome;
        }

        if (scriptTemplates != null && !scriptTemplates.isEmpty()) {
            arguments.scriptTemplates = scriptTemplates.toArray(new String[0]);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (args != null && args.contains("-Xuse-javac")) {
            try {
                URL toolsJar = getJdkToolsJarURL();
                if (toolsJar != null) {
                    project.getClassRealm().addURL(toolsJar);
                }
            } catch (IOException ex) {}
        }

        super.execute();
    }

    @Override
    protected ExitCode execCompiler(
            CLICompiler<K2JVMCompilerArguments> compiler,
            MessageCollector messageCollector,
            K2JVMCompilerArguments arguments,
            List<File> sourceRoots
    ) throws MojoExecutionException {
        if (isIncremental()) {
            return runIncrementalCompiler(messageCollector, arguments, sourceRoots);
        }

        return super.execCompiler(compiler, messageCollector, arguments, sourceRoots);
    }

    @NotNull
    private ExitCode runIncrementalCompiler(
            MessageCollector messageCollector,
            K2JVMCompilerArguments arguments,
            List<File> sourceRoots
    ) throws MojoExecutionException {
        getLog().warn("Using experimental Kotlin incremental compilation");
        File cachesDir = getCachesDir();
        //noinspection ResultOfMethodCallIgnored
        cachesDir.mkdirs();

        MavenICReporter icReporter = MavenICReporter.get(getLog());

        try {
            IncrementalJvmCompilerRunnerKt.makeIncrementally(cachesDir, sourceRoots, arguments, messageCollector, icReporter);

            int compiledKtFilesCount = icReporter.getCompiledKotlinFiles().size();
            getLog().info("Compiled " + icReporter.getCompiledKotlinFiles().size() + " Kotlin files using incremental compiler");
        }
        catch (Throwable t) {
            t.printStackTrace();
            return ExitCode.INTERNAL_ERROR;
        }

        if (messageCollector.hasErrors()) {
            return ExitCode.COMPILATION_ERROR;
        }
        else {
            return ExitCode.OK;
        }
    }

    @Nullable
    private URL getJdkToolsJarURL() throws IOException {
        String javaHomePath = System.getProperty("java.home");
        if (javaHomePath == null || javaHomePath.isEmpty()) {
            return null;
        }
        File javaHome = new File(javaHomePath);
        File toolsJar = new File(javaHome, "lib/tools.jar");
        if (toolsJar.exists()) {
            return toolsJar.getCanonicalFile().toURI().toURL();
        }

        // We might be inside jre.
        if (javaHome.getName().equals("jre")) {
            toolsJar = new File(javaHome.getParent(), "lib/tools.jar");
            if (toolsJar.exists()) {
                return toolsJar.getCanonicalFile().toURI().toURL();
            }
        }

        return null;
    }

}
