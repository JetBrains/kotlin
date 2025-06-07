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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.build.SourcesUtilsKt;
import org.jetbrains.kotlin.buildtools.api.*;
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters;
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration;
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfiguration;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaModule;
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.maven.incremental.FileCopier;
import org.jetbrains.kotlin.maven.kapt.AnnotationProcessingManager;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil.join;
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

    @Parameter(defaultValue = "${plugin}", required = true, readonly = true)
    private PluginDescriptor plugin;

    @NotNull
    private File getCachesDir() {
        return new File(incrementalCachesRoot, getSourceSetName());
    }

    protected boolean isIncremental() {
        return myIncremental;
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

        arguments.setJavaSourceRoots(sourceRoots.stream().map(File::getAbsolutePath).toArray(String[]::new));
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
                    plugin.getClassRealm().addURL(toolsJar);
                }

            } catch (IOException ignored) {}
        }

        super.execute();
    }

    private CompilationService getCompilationService() {
        ClassLoader btaClassloader = this.getClass().getClassLoader(); // load it within the same classloader yet
        return CompilationService.loadImplementation(btaClassloader);
    }

    private static String getFileExtension(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    @Override
    @NotNull
    protected ExitCode execCompiler(
            CLICompiler<K2JVMCompilerArguments> compiler,
            MessageCollector messageCollector,
            K2JVMCompilerArguments arguments,
            List<File> sourceRoots
    ) throws MojoExecutionException {
        try {
            ProjectId projectId = ProjectId.Companion.RandomProjectUUID();
            CompilationService compilationService = getCompilationService();
            CompilerExecutionStrategyConfiguration strategyConfig = compilationService.makeCompilerExecutionStrategyConfiguration();
            strategyConfig.useInProcessStrategy();

            JvmCompilationConfiguration compileConfig = compilationService.makeJvmCompilationConfiguration();

            LegacyKotlinMavenLogger kotlinMavenLogger = new LegacyKotlinMavenLogger(messageCollector, getLog());
            compileConfig.useLogger(kotlinMavenLogger);

            Set<Consumer<CompilationResult>> resultHandlers = new HashSet<>();
            if (isIncremental()) {
                resultHandlers.add(configureIncrementalCompilation(compileConfig, arguments));
            }

            Set<String> kotlinExtensions = SourcesUtilsKt.getDEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS();
            Set<String> allExtensions = new HashSet<>(kotlinExtensions);
            allExtensions.add("java");

            List<File> allSources = new ArrayList<>();
            for (File sourceRoot : sourceRoots) {
                try (Stream<Path> files = Files.walk(sourceRoot.toPath())) {
                    allSources.addAll(
                            files
                                    .map(Path::toFile)
                                    .filter(file -> allExtensions.contains(getFileExtension(file).toLowerCase(Locale.ROOT)))
                                    .filter(File::isFile)
                                    .collect(Collectors.toList())
                    );
                }
            }
            List<String> myArguments = ArgumentUtils.convertArgumentsToStringList(arguments);

            CompilationResult result = compilationService.compileJvm(projectId, strategyConfig, compileConfig, allSources, myArguments);
            compilationService.finishProjectCompilation(projectId);
            resultHandlers.forEach(handler -> handler.accept(result));
            switch (result) {
                case COMPILATION_SUCCESS:
                    return ExitCode.OK;
                case COMPILATION_ERROR:
                    return ExitCode.COMPILATION_ERROR;
                case COMPILATION_OOM_ERROR:
                    return ExitCode.OOM_ERROR;
                default:
                    return ExitCode.INTERNAL_ERROR;
            }
        } catch (Throwable t) {
            getLog().error("Internal Kotlin compilation error", t);
            return ExitCode.INTERNAL_ERROR;
        }
    }

    private Consumer<CompilationResult> configureIncrementalCompilation(
            JvmCompilationConfiguration compileConfig,
            K2JVMCompilerArguments arguments
    ) {
        getLog().warn("Using experimental Kotlin incremental compilation");
        File cachesDir = getCachesDir();
        //noinspection ResultOfMethodCallIgnored
        cachesDir.mkdirs();
        String originalDestination = arguments.getDestination();
        assert originalDestination != null : "output is not specified!";
        File classesDir = new File(originalDestination);
        File kotlinClassesDir = new File(cachesDir, "classes");
        File snapshotsFile = new File(cachesDir, "snapshots.bin");
        String originalClasspath = arguments.getClasspath();

        arguments.setDestination(kotlinClassesDir.getAbsolutePath());
        if (originalClasspath != null) {
            List<String> filteredClasspath = new ArrayList<>();
            for (String path : originalClasspath.split(File.pathSeparator)) {
                if (!classesDir.equals(new File(path))) {
                    filteredClasspath.add(path);
                }
            }
            arguments.setClasspath(StringUtil.join(filteredClasspath, File.pathSeparator));
        }

        ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration icConf =
                compileConfig.makeClasspathSnapshotBasedIncrementalCompilationConfiguration();
        ClasspathSnapshotBasedIncrementalCompilationApproachParameters classpathSnapshotParams =
                new ClasspathSnapshotBasedIncrementalCompilationApproachParameters(Collections.EMPTY_LIST,
                                                                                   new File(cachesDir, "shrunk-classpath-snapshot.bin"));
        compileConfig.useIncrementalCompilation(cachesDir, SourcesChanges.ToBeCalculated.INSTANCE, classpathSnapshotParams, icConf);

        return compilationResult -> {
            if (compilationResult == CompilationResult.COMPILATION_SUCCESS) {
                (new FileCopier(getLog())).syncDirs(kotlinClassesDir, classesDir, snapshotsFile);
            }
            arguments.setDestination(originalDestination);
            arguments.setClasspath(originalClasspath);
        };
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
