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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiJavaModule;
import kotlin.collections.MapsKt;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.incremental.CompilerRunnerUtils;
import org.jetbrains.kotlin.maven.incremental.FileCopier;
import org.jetbrains.kotlin.maven.incremental.MavenICReporter;
import org.jetbrains.kotlin.maven.kapt.AnnotationProcessingManager;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

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

    @Parameter(property = "kotlin.compiler.jvmTarget")
    protected String jvmTarget;

    @Parameter(property = "kotlin.compiler.jdkHome")
    protected String jdkHome;

    @Parameter(property = "kotlin.compiler.scriptTemplates")
    protected List<String> scriptTemplates;

    @Parameter(property = "kotlin.compiler.incremental", defaultValue = "false")
    private boolean myIncremental;

    @Parameter(property = "kotlin.compiler.incremental.cache.root", defaultValue = "${project.build.directory}/kotlin-ic")
    public String incrementalCachesRoot;

    @Parameter(property = "kotlin.compiler.javaParameters")
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
        List<String> paths = new ArrayList<>(super.getSourceFilePaths());

        File sourcesDir = AnnotationProcessingManager.getGeneratedSourcesDirectory(project, getSourceSetName());
        if (sourcesDir.isDirectory()) {
            paths.add(sourcesDir.getAbsolutePath());
        }

        File kotlinSourcesDir = AnnotationProcessingManager.getGeneratedKotlinSourcesDirectory(project, getSourceSetName());
        if (kotlinSourcesDir.isDirectory()) {
            paths.add(kotlinSourcesDir.getAbsolutePath());
        }

        return paths;
    }

    protected List<String> getClasspath() {
        return filterClassPath(project.getBasedir(), classpath);
    }

    @NotNull
    protected String getSourceSetName() {
        return AnnotationProcessingManager.COMPILE_SOURCE_SET_NAME;
    }

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2JVMCompilerArguments arguments, @NotNull List<File> sourceRoots) throws MojoExecutionException {
        arguments.setDestination(output);

        // don't include runtime, it should be in maven dependencies
        arguments.setNoStdlib(true);
        arguments.setJavaParameters(this.javaParameters);

        //noinspection deprecation
        if (module != null || testModule != null) {
            getLog().warn("Parameters module and testModule are deprecated and ignored, they will be removed in further release.");
        }

        List<String> classpathList = getClasspath();

        if (!classpathList.isEmpty()) {
            String classPathString = join(classpathList, File.pathSeparator);
            if (isJava9Module(sourceRoots)) {
                getLog().debug("Module path: " + classPathString);
                arguments.setJavaModulePath(classPathString);
            }
            else {
                getLog().debug("Classpath: " + classPathString);
                arguments.setClasspath(classPathString);
            }
        }

        getLog().debug("Classes directory is " + output);
        arguments.setDestination(output);

        arguments.setModuleName(moduleName);
        getLog().debug("Module name is " + moduleName);

        if (arguments.getNoOptimize()) {
            getLog().info("Optimization is turned off");
        }

        if (jvmTarget != null) {
            arguments.setJvmTarget(jvmTarget);
        } else {
            arguments.setJvmTarget(JvmTarget.DEFAULT.getDescription());
        }

        if (jdkHome != null) {
            getLog().info("Overriding JDK home path with: " + jdkHome);
            arguments.setJdkHome(jdkHome);
        }

        if (scriptTemplates != null && !scriptTemplates.isEmpty()) {
            arguments.setScriptTemplates(scriptTemplates.toArray(new String[0]));
        }
    }

    private boolean isJava9Module(@NotNull List<File> sourceRoots) {
        //noinspection ConstantConditions
        return sourceRoots.stream().anyMatch(file ->
                file.getName().equals(PsiJavaModule.MODULE_INFO_FILE) ||
                file.isDirectory() && Arrays.stream(file.listFiles()).anyMatch(child ->
                        child.getName().equals(PsiJavaModule.MODULE_INFO_FILE)
                )
        );
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (args != null && args.contains("-Xuse-javac")) {
            try {
                URL toolsJar = getJdkToolsJarURL();
                if (toolsJar != null) {
                    project.getClassRealm().addURL(toolsJar);
                }
            } catch (IOException ignored) {}
        }

        super.execute();
    }

    @Override
    @NotNull
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
        String destination = arguments.getDestination();
        assert destination != null : "output is not specified!";
        File classesDir = new File(destination);
        File kotlinClassesDir = new File(cachesDir, "classes");
        File snapshotsFile = new File(cachesDir, "snapshots.bin");
        String classpath = arguments.getClasspath();
        MavenICReporter icReporter = new MavenICReporter(getLog());

        try {
            arguments.setDestination(kotlinClassesDir.getAbsolutePath());
            if (classpath != null) {
                List<String> filteredClasspath = new ArrayList<>();
                for (String path : classpath.split(File.pathSeparator)) {
                    if (!classesDir.equals(new File(path))) {
                        filteredClasspath.add(path);
                    }
                }
                arguments.setClasspath(StringUtil.join(filteredClasspath, File.pathSeparator));
            }

                CompilerRunnerUtils.makeJvmIncrementally(cachesDir, sourceRoots, arguments, messageCollector, icReporter);

            int compiledKtFilesCount = icReporter.getCompiledKotlinFiles().size();
            getLog().info("Compiled " + icReporter.getCompiledKotlinFiles().size() + " Kotlin files using incremental compiler");

            if (!messageCollector.hasErrors()) {
                (new FileCopier(getLog())).syncDirs(kotlinClassesDir, classesDir, snapshotsFile);
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
            return ExitCode.INTERNAL_ERROR;
        }
        finally {
            arguments.setDestination(destination);
            arguments.setClasspath(classpath);
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
