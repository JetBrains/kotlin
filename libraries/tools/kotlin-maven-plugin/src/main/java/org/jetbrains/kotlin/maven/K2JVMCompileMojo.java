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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.build.SourcesUtilsKt;
import org.jetbrains.kotlin.buildtools.api.*;
import org.jetbrains.kotlin.buildtools.api.jvm.*;
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation;
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

import javax.inject.Inject;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil.join;
import static org.jetbrains.kotlin.maven.Util.filterClassPath;
import static org.jetbrains.kotlin.maven.Util.getMavenPluginVersion;

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

    @Parameter(property = "kotlin.compiler.daemon", defaultValue = "true")
    protected boolean useDaemon;

    @Parameter(property = "kotlin.compiler.classloader.cache.timeoutSeconds")
    @Nullable
    protected Long classLoaderCacheTimeoutSeconds;

    private static final Duration DEFAULT_CLASSLOADER_CACHE_TIMEOUT = Duration.ofMinutes(30);

    @Parameter(property = "kotlin.compiler.daemon.jvmArgs")
    protected List<String> kotlinDaemonJvmArgs;

    @Parameter(property = "kotlin.compiler.daemon.shutdownDelayMs")
    protected Long daemonShutdownDelayMs;

    @Parameter(property = "kotlin.compiler.generateCompilerRefIndex", defaultValue = "false")
    protected boolean generateCompilerRefIndex;

    /**
     * The time the Kotlin daemon continues to live after the Maven build process finishes (without the Maven daemon)
     */
    private static final Duration DEFAULT_NON_MAVEN_DAEMON_SHUTDOWN_DELAY = Duration.ofMinutes(30);
    /**
     * The time the Kotlin daemon continues to live after the Maven daemon shuts down
     */
    private static final Duration DEFAULT_MAVEN_DAEMON_SHUTDOWN_DELAY = Duration.ofSeconds(1);
    /**
     * A system property used to detect we are inside the Maven Daemon.
     */
    private static final String MAVEN_DAEMON_PROPERTY_NAME = "mvnd.home";

    @NotNull
    private Path getCachesDir() {
        return Paths.get(incrementalCachesRoot, getSourceSetName());
    }

    @NotNull
    private Path getKotlinClassesCacheDir() {
        return getCachesDir().resolve("classes");
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

    @Inject
    private KotlinArtifactResolver kotlinArtifactResolver;

    private KotlinToolchains getKotlinToolchains() throws MojoExecutionException {
        try {
            //Set<Artifact> artifacts =
            //        Stream.concat(
            //                kotlinArtifactResolver.resolveArtifact("org.jetbrains.kotlin", "kotlin-build-tools-impl", getMavenPluginVersion()).stream(),
            //                kotlinArtifactResolver.resolveArtifact("org.jetbrains.kotlin", "kotlin-scripting-compiler-embeddable", getMavenPluginVersion()).stream()
            //        ).collect(Collectors.toCollection(LinkedHashSet::new));
            //List<File> files = artifacts.stream().map(Artifact::getFile).collect(Collectors.toList());
            //ClassLoader btaClassLoader = getBtaClassLoader(files);
            return KotlinToolchains.loadImplementation(getClass().getClassLoader());
        } catch (Throwable t) {
            throw new MojoExecutionException("Failed to load Kotlin Build Tools API implementation", t);
        }
    }

    private ClassLoader getBtaClassLoader(List<File> files) {
        ClassLoaderCache.ClassLoaderCacheKey cacheKey =
                new ClassLoaderCache.ClassLoaderCacheKey(files, new SharedBuildToolsApiClassesClassLoaderProvider());
        try {
            long cacheTimeout = classLoaderCacheTimeoutSeconds == null
                                ? DEFAULT_CLASSLOADER_CACHE_TIMEOUT.getSeconds()
                                : classLoaderCacheTimeoutSeconds;
            return ClassLoaderCache.getCache(cacheTimeout).get(cacheKey, () -> {
                getLog().debug("Creating classloader for " + cacheKey.getClasspath());
                URL[] urls = cacheKey.getClasspath().stream().map(file -> {
                    try {
                        return file.toURI().toURL();
                    }
                    catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }).toArray(URL[]::new);
                //return new URLClassLoader(urls, cacheKey.getParentClassLoaderProvider().getClassLoader());
                return new URLClassLoader(urls, getClass().getClassLoader());
            });
        }
        catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFileExtension(Path file) {
        String fileName = file.getFileName().toString();
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
            KotlinToolchains kotlinToolchains = getKotlinToolchains();
            ExecutionPolicy executionPolicy;
            if (useDaemon) {
                boolean inMavenDaemon = System.getProperty(MAVEN_DAEMON_PROPERTY_NAME) != null;
                Duration usedDaemonShutdownDelay;
                if (daemonShutdownDelayMs != null) {
                    // respect explicitly specified value
                    usedDaemonShutdownDelay = Duration.ofMillis(daemonShutdownDelayMs);
                } else if (inMavenDaemon) {
                    usedDaemonShutdownDelay = DEFAULT_MAVEN_DAEMON_SHUTDOWN_DELAY;
                } else {
                    usedDaemonShutdownDelay = DEFAULT_NON_MAVEN_DAEMON_SHUTDOWN_DELAY;
                }
                getLog().debug("Using Kotlin compiler daemon with shutdown delay " + usedDaemonShutdownDelay + " ms" + (inMavenDaemon ? " (in Maven daemon)" : " (outside Maven daemon)"));
                ExecutionPolicy.WithDaemon daemonPolicy = kotlinToolchains.createDaemonExecutionPolicy();
                daemonPolicy.set(ExecutionPolicy.WithDaemon.JVM_ARGUMENTS, kotlinDaemonJvmArgs);
                daemonPolicy.set(ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS, usedDaemonShutdownDelay.toMillis());
                executionPolicy = daemonPolicy;
            } else {
                getLog().debug("Using in-process Kotlin compiler");
                executionPolicy = kotlinToolchains.createInProcessExecutionPolicy();
            }

            JvmPlatformToolchain jvmToolchain = JvmPlatformToolchain.from(kotlinToolchains);
            Set<String> kotlinExtensions = SourcesUtilsKt.getDEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS();
            Set<String> allExtensions = new HashSet<>(kotlinExtensions);
            allExtensions.add("java");

            List<Path> allSources = new ArrayList<>();
            for (File sourceRoot : sourceRoots) {
                try (Stream<Path> files = Files.walk(sourceRoot.toPath())) {
                    allSources.addAll(
                            files
                                    .filter(file -> allExtensions.contains(getFileExtension(file).toLowerCase(Locale.ROOT)))
                                    .filter(Files::isRegularFile)
                                    .collect(Collectors.toList())
                    );
                }
            }
            List<String> myArguments = ArgumentUtils.convertArgumentsToStringList(arguments);

            Set<Consumer<CompilationResult>> resultHandlers = new HashSet<>();

            Path destination = getEffectiveDestinationDirectory(arguments);
            JvmCompilationOperation compilationOperation = jvmToolchain.createJvmCompilationOperation(allSources, destination);

            if (isIncremental()) {
                resultHandlers.add(configureIncrementalCompilation(compilationOperation, arguments));
            }

            compilationOperation.set(JvmCompilationOperation.GENERATE_COMPILER_REF_INDEX, generateCompilerRefIndex);

            LegacyKotlinMavenLogger kotlinMavenLogger = new LegacyKotlinMavenLogger(messageCollector, getLog());
            try (KotlinToolchains.BuildSession buildSession = kotlinToolchains.createBuildSession()) {
                compilationOperation.getCompilerArguments().applyArgumentStrings(myArguments);
                CompilationResult result = buildSession.executeOperation(compilationOperation, executionPolicy, kotlinMavenLogger);
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
            }
        } catch (Throwable t) {
            getLog().error("Internal Kotlin compilation error", t);
            return ExitCode.INTERNAL_ERROR;
        }
    }

    private Path getEffectiveDestinationDirectory(K2JVMCompilerArguments arguments) {
        if (isIncremental()) {
            return getKotlinClassesCacheDir();
        } else {
            String destination = Objects.requireNonNull(arguments.getDestination());
            return Paths.get(destination);
        }
    }

    private Consumer<CompilationResult> configureIncrementalCompilation(
            JvmCompilationOperation compileOperation,
            K2JVMCompilerArguments arguments
    ) throws IOException {
        getLog().warn("Using experimental Kotlin incremental compilation");
        Path cachesDir = getCachesDir();
        Files.createDirectories(cachesDir);
        String originalDestination = arguments.getDestination();
        assert originalDestination != null : "output is not specified!";
        File classesDir = new File(originalDestination);
        File kotlinClassesDir = getKotlinClassesCacheDir().toFile();
        File snapshotsFile = new File(cachesDir.toFile(), "snapshots.bin");
        String originalClasspath = arguments.getClasspath();

        if (originalClasspath != null) {
            List<String> filteredClasspath = new ArrayList<>();
            for (String path : originalClasspath.split(File.pathSeparator)) {
                if (!classesDir.equals(new File(path))) {
                    filteredClasspath.add(path);
                }
            }
            arguments.setClasspath(StringUtil.join(filteredClasspath, File.pathSeparator));
        }

        JvmSnapshotBasedIncrementalCompilationOptions classpathSnapshotsOptions = compileOperation.createSnapshotBasedIcOptions();
        compileOperation.set(JvmCompilationOperation.INCREMENTAL_COMPILATION, new JvmSnapshotBasedIncrementalCompilationConfiguration(
                cachesDir,
                SourcesChanges.ToBeCalculated.INSTANCE,
                Collections.EMPTY_LIST,
                cachesDir.resolve("shrunk-classpath-snapshot.bin"),
                classpathSnapshotsOptions
        ));

        return compilationResult -> {
            if (compilationResult == CompilationResult.COMPILATION_SUCCESS) {
                (new FileCopier(getLog())).syncDirs(kotlinClassesDir, classesDir, snapshotsFile);
            }
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
