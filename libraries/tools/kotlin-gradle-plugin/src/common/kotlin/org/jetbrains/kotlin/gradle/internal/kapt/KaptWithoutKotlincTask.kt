/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.internal.kapt.classloaders.ClassLoadersCache
import org.jetbrains.kotlin.gradle.internal.kapt.classloaders.rootOrSelf
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptIncrementalChanges
import org.jetbrains.kotlin.gradle.tasks.Kapt
import org.jetbrains.kotlin.gradle.tasks.toSingleCompilerPluginOptions
import org.jetbrains.kotlin.gradle.utils.listPropertyWithConvention
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.PathUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.io.Serializable
import java.net.URL
import java.net.URLClassLoader
import javax.inject.Inject

@CacheableTask
abstract class KaptWithoutKotlincTask @Inject constructor(
    objectFactory: ObjectFactory,
    private val providerFactory: ProviderFactory,
    private val workerExecutor: WorkerExecutor
) : KaptTask(objectFactory), Kapt {

    @get:Input
    var classLoadersCacheSize: Int = 0

    @get:Input
    var disableClassloaderCacheForProcessors: Set<String> = emptySet()

    @get:Input
    var mapDiagnosticLocations: Boolean = false

    @get:Input
    abstract val annotationProcessorFqNames: ListProperty<String>

    @get:Input
    abstract val javacOptions: MapProperty<String, String>

    @get:Internal
    internal val projectDir = project.projectDir

    @get:Input
    val kaptProcessJvmArgs: ListProperty<String> = objectFactory.listPropertyWithConvention(emptyList())

    init {
        // Skip annotation processing if no annotation processors were provided.
        onlyIf { task ->
            with(task as KaptWithoutKotlincTask) {
                val isRunTask = !(annotationProcessorFqNames.get().isEmpty() && kaptClasspath.isEmpty())
                if (!isRunTask) task.logger.info("No annotation processors provided. Skip KAPT processing.")
                isRunTask
            }
        }
    }

    private fun getAnnotationProcessorOptions(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        kaptPluginOptions.toSingleCompilerPluginOptions().subpluginOptionsByPluginId[Kapt3GradleSubplugin.KAPT_SUBPLUGIN_ID]?.forEach {
            result[it.key] = it.value
        }
        annotationProcessorOptionProviders.forEach { providers ->
            (providers as List<*>).forEach { provider ->
                (provider as CommandLineArgumentProvider).asArguments().forEach { argument ->
                    result[argument.removePrefix("-A")] = ""
                }
            }
        }

        return result
    }

    @TaskAction
    fun compile(inputChanges: InputChanges) {
        logger.info("Running kapt annotation processing using the Gradle Worker API")
        checkProcessorCachingSetup()
        checkAnnotationProcessorClasspath()

        val incrementalChanges = getIncrementalChanges(inputChanges)
        val (changedFiles, classpathChanges) = when (incrementalChanges) {
            is KaptIncrementalChanges.Unknown -> Pair(emptyList<File>(), emptyList<String>())
            is KaptIncrementalChanges.Known -> Pair(incrementalChanges.changedSources.toList(), incrementalChanges.changedClasspathJvmNames)
        }

        val compileClasspath = classpath.files.toMutableList()
        if (addJdkClassesToClasspath.get()) {
            compileClasspath.addAll(
                0,
                PathUtil.getJdkClassesRoots(defaultKotlinJavaToolchain.get().buildJvm.get().javaHome)
            )
        }

        val kaptFlagsForWorker = mutableSetOf<String>().apply {
            if (verbose.get()) add("VERBOSE")
            if (mapDiagnosticLocations) add("MAP_DIAGNOSTIC_LOCATIONS")
            if (includeCompileClasspath.get()) add("INCLUDE_COMPILE_CLASSPATH")
            if (incrementalChanges is KaptIncrementalChanges.Known) add("INCREMENTAL_APT")
        }

        val optionsForWorker = KaptOptionsForWorker(
            projectDir,
            compileClasspath,
            source.files.toList(),

            changedFiles,
            compiledSources.toList(),
            incAptCache.orNull?.asFile,
            classpathChanges.toList(),

            destinationDir.get().asFile,
            classesDir.get().asFile,
            stubsDir.asFile.get(),

            kaptClasspath.files.toList(),
            kaptExternalClasspath.files.toList(),
            annotationProcessorFqNames.get(),

            getAnnotationProcessorOptions(),
            javacOptions.get(),

            kaptFlagsForWorker,

            disableClassloaderCacheForProcessors
        )

        val kaptClasspath = kaptJars
        val isolationMode = getWorkerIsolationMode()
        logger.info("Using workers $isolationMode isolation mode to run kapt")
        val toolsJarURLSpec = defaultKotlinJavaToolchain.get()
            .jdkToolsJar.orNull?.toURI()?.toURL()?.toString().orEmpty()

        submitWork(
            isolationMode,
            optionsForWorker,
            toolsJarURLSpec,
            kaptClasspath
        )
    }

    private fun getWorkerIsolationMode(): IsolationMode {
        val toolchainProvider = defaultKotlinJavaToolchain.get()
        val gradleJvm = toolchainProvider.gradleJvm.get()
        // Ensuring Gradle build JDK is set to kotlin toolchain by also comparing javaExecutable paths,
        // as user may set JDK with same major Java version, but from different vendor
        val isRunningOnGradleJvm = gradleJvm.javaVersion == toolchainProvider.javaVersion.get() &&
                gradleJvm.javaExecutable.absolutePath == toolchainProvider.javaExecutable.get().asFile.absolutePath
        val isolationModeStr = getValue("kapt.workers.isolation")?.toLowerCaseAsciiOnly()
        return when {
            (isolationModeStr == null || isolationModeStr == "none") && isRunningOnGradleJvm -> IsolationMode.NONE
            else -> {
                if (isolationModeStr == "none") {
                    logger.warn("Using non-default Kotlin java toolchain - 'kapt.workers.isolation == none' property is ignored!")
                }
                IsolationMode.PROCESS
            }
        }
    }

    private fun submitWork(
        isolationMode: IsolationMode,
        optionsForWorker: KaptOptionsForWorker,
        toolsJarURLSpec: String,
        kaptClasspath: FileCollection
    ) {
        val workQueue = when (isolationMode) {
            IsolationMode.PROCESS -> workerExecutor.processIsolation {
                if (getValue("kapt.workers.log.classloading") == "true") {
                    // for tests
                    it.forkOptions.jvmArgs("-verbose:class")
                }
                kaptProcessJvmArgs.get().run { if (isNotEmpty()) it.forkOptions.jvmArgs(this) }
                it.forkOptions.executable = defaultKotlinJavaToolchain.get()
                    .javaExecutable
                    .asFile.get()
                    .absolutePath
                logger.info("Kapt worker classpath: ${kaptClasspath.toList()}")
            }
            IsolationMode.NONE -> {
                warnAdditionalJvmArgsAreNotUsed(isolationMode)
                workerExecutor.noIsolation()
            }
            IsolationMode.AUTO, IsolationMode.CLASSLOADER -> throw UnsupportedOperationException(
                "Kapt worker compilation does not support class loader isolation. " +
                        "Please use either \"none\" or \"process\" in gradle.properties."
            )
        }

        workQueue.submit(KaptExecutionWorkAction::class.java) {
            it.workerOptions.set(optionsForWorker)
            it.toolsJarURLSpec.set(toolsJarURLSpec)
            it.kaptClasspath.setFrom(kaptClasspath)
            it.classloadersCacheSize.set(classLoadersCacheSize)
        }
    }

    private fun warnAdditionalJvmArgsAreNotUsed(isolationMode: IsolationMode) {
        if (kaptProcessJvmArgs.get().isNotEmpty()) {
            logger.warn("Kapt additional JVM arguments are ignored in '${isolationMode.name}' workers isolation mode")
        }
    }

    private fun getValue(propertyName: String): String? = providerFactory.gradleProperty(propertyName).orNull

    internal interface KaptWorkParameters : WorkParameters {
        val workerOptions: Property<KaptOptionsForWorker>
        val toolsJarURLSpec: Property<String>
        val kaptClasspath: ConfigurableFileCollection
        val classloadersCacheSize: Property<Int>
    }

    /**
     * Copied over Gradle deprecated [IsolationMode] enum, so Gradle could remove it.
     */
    internal enum class IsolationMode {
        /**
         * Let Gradle decide, this is the default.
         */
        AUTO,

        /**
         * Don't attempt to isolate the work, use in-process workers.
         */
        NONE,

        /**
         * Isolate the work in it's own classloader, use in-process workers.
         */
        CLASSLOADER,

        /**
         * Isolate the work in a separate process, use out-of-process workers.
         */
        PROCESS
    }

    internal abstract class KaptExecutionWorkAction : WorkAction<KaptWorkParameters> {
        override fun execute() {
            KaptExecution(
                parameters.workerOptions.get(),
                parameters.toolsJarURLSpec.get(),
                parameters.kaptClasspath.toList(),
                parameters.classloadersCacheSize.get()
            ).run()
        }
    }

    private fun checkProcessorCachingSetup() {
        if (includeCompileClasspath.get() && classLoadersCacheSize > 0) {
            logger.warn(
                "ClassLoaders cache can't be enabled together with AP discovery in compilation classpath."
                        + "\nSet 'kapt.include.compile.classpath=false' to disable discovery"
            )
        }
    }
}


private class KaptExecution @Inject constructor(
    val optionsForWorker: KaptOptionsForWorker,
    val toolsJarURLSpec: String,
    val kaptClasspath: List<File>,
    val classloadersCacheSize: Int
) : Runnable {
    private companion object {
        private const val JAVAC_CONTEXT_CLASS = "com.sun.tools.javac.util.Context"

        private fun ClassLoader.kaptClass(simpleName: String): Class<*> =
            try {
                Class.forName("org.jetbrains.kotlin.kapt3.base.$simpleName", true, this)
            } catch (_: ClassNotFoundException) { // in case we have an old plugin version on the classpath
                Class.forName("org.jetbrains.kotlin.base.kapt3.$simpleName", true, this)
            }

        private var classLoadersCache: ClassLoadersCache? = null

        private var cachedKaptClassLoader: ClassLoader? = null
    }

    private val logger = LoggerFactory.getLogger(KaptExecution::class.java)

    override fun run(): Unit = with(optionsForWorker) {
        val kaptClasspathUrls = kaptClasspath.map { it.toURI().toURL() }.toTypedArray()
        val rootClassLoader = findRootClassLoader()

        val kaptClassLoader = cachedKaptClassLoader ?: run {
            val classLoaderWithToolsJar = if (toolsJarURLSpec.isNotEmpty() && !javacIsAlreadyHere()) {
                URLClassLoader(arrayOf(URL(toolsJarURLSpec)), rootClassLoader)
            } else {
                rootClassLoader
            }
            val result = URLClassLoader(kaptClasspathUrls, classLoaderWithToolsJar)
            cachedKaptClassLoader = result
            result
        }

        if (classLoadersCache == null && classloadersCacheSize > 0) {
            logger.info("Initializing KAPT classloaders cache with size = $classloadersCacheSize")
            classLoadersCache = ClassLoadersCache(classloadersCacheSize, kaptClassLoader)
        }

        val kaptMethod = kaptClassLoader.kaptClass("Kapt").declaredMethods.single { it.name == "kapt" }
        kaptMethod.invoke(null, createKaptOptions(kaptClassLoader))
    }

    private fun javacIsAlreadyHere(): Boolean {
        return try {
            Class.forName(JAVAC_CONTEXT_CLASS, false, KaptExecution::class.java.classLoader) != null
        } catch (e: Throwable) {
            false
        }
    }

    private fun createKaptOptions(classLoader: ClassLoader): Any = with(optionsForWorker) {
        val flags = classLoader.kaptClass("Kapt").declaredMethods.single { it.name == "kaptFlags" }.invoke(null, flags)

        val mode = classLoader.kaptClass("AptMode")
            .enumConstants.single { (it as Enum<*>).name == "APT_ONLY" }

        val detectMemoryLeaksMode = classLoader.kaptClass("DetectMemoryLeaksMode")
            .enumConstants.single { (it as Enum<*>).name == "NONE" }

        //in case cache was enabled and then disabled
        //or disabled for some modules
        val processingClassLoader =
            if (classloadersCacheSize > 0) {
                classLoadersCache!!.getForSplitPaths(processingClasspath - processingExternalClasspath, processingExternalClasspath)
            } else {
                null
            }

        classLoader.kaptClass("KaptOptions").constructors.single().newInstance(
            projectBaseDir,
            compileClasspath,
            javaSourceRoots,

            changedFiles,
            compiledSources,
            incAptCache,
            classpathChanges,

            sourcesOutputDir,
            classesOutputDir,
            stubsOutputDir,
            stubsOutputDir, // sic!

            processingClasspath,
            processors,

            processingOptions,
            javacOptions,

            flags,
            mode,
            detectMemoryLeaksMode,

            processingClassLoader,
            disableClassloaderCacheForProcessors,
            /*processorsPerfReportFile=*/null,
            /*fileReadHistoryReportFile*/null,
        )
    }

    private fun findRootClassLoader(): ClassLoader = KaptExecution::class.java.classLoader.rootOrSelf()
}

internal data class KaptOptionsForWorker(
    val projectBaseDir: File,
    val compileClasspath: List<File>,
    val javaSourceRoots: List<File>,

    val changedFiles: List<File>,
    val compiledSources: List<File>,
    val incAptCache: File?,
    val classpathChanges: List<String>,

    val sourcesOutputDir: File,
    val classesOutputDir: File,
    val stubsOutputDir: File,

    val processingClasspath: List<File>,
    val processingExternalClasspath: List<File>,
    val processors: List<String>,

    val processingOptions: Map<String, String>,
    val javacOptions: Map<String, String>,

    val flags: Set<String>,

    val disableClassloaderCacheForProcessors: Set<String>
) : Serializable
