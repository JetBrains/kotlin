/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.buildtools.internal.KotlinBuildToolsInternalJdkUtils
import org.jetbrains.kotlin.buildtools.internal.getJdkClassesClassLoader
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.argumentAnnotation
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerArgumentsLogLevel
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.ParentClassLoaderProvider
import org.jetbrains.kotlin.gradle.logging.GradleErrorMessageCollector
import org.jetbrains.kotlin.gradle.logging.gradleLogLevel
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFusService
import org.jetbrains.kotlin.gradle.report.TaskExecutionResult
import org.jetbrains.kotlin.gradle.utils.escapeStringCharacters
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.statistics.FusMetricRetrievalException
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.inject.Inject

internal abstract class KotlinNativeToolRunner @Inject constructor(
    private val metricsReporterProvider: Provider<BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric>>,
    private val classLoadersCachingBuildServiceProvider: Provider<ClassLoadersCachingBuildService>,
    private val toolSpec: ToolSpec,
    private val fusMetricsConsumer: Provider<out BuildFusService<out BuildFusService.Parameters>>,
    private val execOperations: ExecOperations,
    private val workerExecutor: WorkerExecutor,
) {

    companion object {
        private val dumpPerfArgument = CommonCompilerArguments::dumpPerf.argumentAnnotation.value
    }

    private val logger = Logging.getLogger(toolSpec.displayName.get())
    private val errorMessageCollector = GradleErrorMessageCollector(logger)
    private val classLoadersCachingBuildService: ClassLoadersCachingBuildService
        get() = classLoadersCachingBuildServiceProvider.get()
    private val metricsReporter get() = metricsReporterProvider.get()

    fun runTool(args: ToolArguments) {
        metricsReporter.measure(RUN_COMPILATION_IN_WORKER) {
            if (args.shouldRunInProcessMode) {
                runInProcess(args)
            } else {
                runViaExec(args)
            }
        }
    }

    /**
     * Submits a single [runTool] invocation to a Gradle [org.gradle.workers.WorkQueue], so that several
     * independent [KotlinNativeToolRunner] invocations (e.g. one per Kotlin/Native link/compile/commonize
     * task) can execute concurrently instead of serially as part of each task's own action.
     *
     * `noIsolation` is used by default: Gradle still isolates (deep-copies) the submitted [KotlinNativeToolWorkParameters],
     * so this runner (and the build services it wraps) can't be passed directly - instead, only plain,
     * serializable values and build-service references are captured here, and a fresh [KotlinNativeToolRunner]
     * is rebuilt from them inside [KotlinNativeToolWorkAction.execute]. See [KotlinNativeToolWorkAction] for details.
     *
     * [taskPath], when provided, lets [KotlinNativeToolWorkAction] hand the metrics it collects back to the
     * submitting task via [org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults] - this is
     * needed because [metricsReporterProvider] itself is isolated (deep-copied) by Gradle along with the rest of
     * [KotlinNativeToolWorkParameters], so mutating it inside the worker never reaches the original, live reporter.
     * Pass `null` when there is no actual task to report back to.
     */
    fun runToolWithWorkers(args: ToolArguments, taskPath: String? = null): WorkQueue =
        workerExecutor.noIsolation().apply {
            submit(KotlinNativeToolWorkAction::class.java) { it.from(this@KotlinNativeToolRunner, args, taskPath) }
        }

    private fun KotlinNativeToolWorkParameters.from(
        runner: KotlinNativeToolRunner,
        args: ToolArguments,
        taskPath: String?,
    ) {
        classLoadersCachingService.set(runner.classLoadersCachingBuildServiceProvider)
        fusService.set(runner.fusMetricsConsumer)

        val toolSpec = runner.toolSpec
        displayName.set(toolSpec.displayName)
        optionalToolName.set(toolSpec.optionalToolName)
        mainClass.set(toolSpec.mainClass)
        daemonEntryPoint.set(toolSpec.daemonEntryPoint)
        classpath.from(toolSpec.classpath)
        jvmArgs.set(toolSpec.jvmArgs)
        shouldPassArgumentsViaArgFile.set(toolSpec.shouldPassArgumentsViaArgFile)
        collectNativeCompilerMetrics.set(toolSpec.collectNativeCompilerMetrics)
        systemProperties.set(toolSpec.systemProperties)
        environment.set(toolSpec.environment)
        environmentBlacklist.set(toolSpec.environmentBlacklist)

        shouldRunInProcessMode.set(args.shouldRunInProcessMode)
        compilerArgumentsLogLevel.set(args.compilerArgumentsLogLevel)
        arguments.set(args.arguments)

        this.taskPath.set(taskPath)
    }

    private fun runViaExec(args: ToolArguments) {
        metricsReporter.measure(NATIVE_IN_EXECUTOR) {
            val systemProperties = System.getProperties()
                /* Capture 'System.getProperties()' current state to avoid potential 'ConcurrentModificationException' */
                .snapshot()
                .asSequence()
                .map { (k, v) -> k.toString() to v.toString() }
                .filter { (k, _) -> k !in toolSpec.systemPropertiesBlacklist }
                .escapeQuotesForWindows()
                .toMap() + toolSpec.systemProperties

            val reportFile = File.createTempFile("native_compiler_${::runViaExec.name}_report", ".json")
            val reportArguments = if (toolSpec.collectNativeCompilerMetrics.get()) {
                listOf("$dumpPerfArgument=${reportFile.absolutePath}")
            } else emptyList()

            val toolArgsPair = if (toolSpec.shouldPassArgumentsViaArgFile.get()) {
                val argFile = args.toArgFile(reportArguments)
                argFile to listOfNotNull(
                    toolSpec.optionalToolName.orNull,
                    "@${argFile.toFile().absolutePath}"
                )
            } else {
                null to listOfNotNull(toolSpec.optionalToolName.orNull) + args.arguments + reportArguments
            }

            try {
                logger.log(
                    args.compilerArgumentsLogLevel.gradleLogLevel,
                    """
                |Run "${toolSpec.displayName.get()}" tool in a separate JVM process
                |Main class = ${toolSpec.mainClass.get()}
                |Arguments = ${args.arguments.toPrettyString()}
                |Transformed arguments = ${toolArgsPair.second.toPrettyString()}
                |Classpath = ${toolSpec.classpath.files.map { it.absolutePath }.toPrettyString()}
                |JVM options = ${toolSpec.jvmArgs.get().toPrettyString()}
                |Java system properties = ${systemProperties.toPrettyString()}
                |Suppressed ENV variables = ${toolSpec.environmentBlacklist.toPrettyString()}
                |Custom ENV variables = ${toolSpec.environment.toPrettyString()}
                """.trimMargin()
                )

                execOperations.javaexec { spec ->
                    spec.mainClass.set(toolSpec.mainClass)
                    spec.classpath = toolSpec.classpath
                    spec.jvmArgs(toolSpec.jvmArgs.get())
                    spec.systemProperties(systemProperties)
                    spec.environment(toolSpec.environment)
                    toolSpec.environmentBlacklist.forEach { spec.environment.remove(it) }
                    spec.args(toolArgsPair.second)
                }
                metricsReporter.parseCompilerMetricsFromFile(reportFile)
            } finally {
                toolArgsPair.first?.let {
                    try {
                        Files.deleteIfExists(it)
                    } catch (_: IOException) {
                    }
                }
            }
        }
    }

    private fun runInProcess(args: ToolArguments) {
        metricsReporter.measure(NATIVE_IN_PROCESS) {
            val isolatedClassLoader = classLoadersCachingBuildService.getClassLoader(
                toolSpec.classpath.files.toList(),
                // Required for KotlinNativePaths to properly detect konan home directory
                object : ParentClassLoaderProvider {
                    @OptIn(KotlinBuildToolsInternalJdkUtils::class)
                    private val jdkClassesClassLoader = getJdkClassesClassLoader()

                    override fun getClassLoader(): ClassLoader? = jdkClassesClassLoader
                    override fun hashCode(): Int = jdkClassesClassLoader.hashCode()
                    override fun equals(other: Any?): Boolean =
                        other is ParentClassLoaderProvider && other.getClassLoader() == jdkClassesClassLoader
                }
            )
            if (toolSpec.jvmArgs.get().contains("-ea")) isolatedClassLoader.setDefaultAssertionStatus(true)

            logger.log(
                args.compilerArgumentsLogLevel.gradleLogLevel,
                """
                |Run in-process tool "${toolSpec.displayName.get()}"
                |Entry point method = ${toolSpec.mainClass.get()}.${toolSpec.daemonEntryPoint.get()}
                |Classpath = ${toolSpec.classpath.files.map { it.absolutePath }.toPrettyString()}
                |Arguments = ${args.arguments.toPrettyString()}
                """.trimMargin()
            )

            val toolArgs = listOf(toolSpec.displayName.get()) + args.arguments
            try {
                val mainClass = isolatedClassLoader.loadClass(toolSpec.mainClass.get())
                val entryPoint = mainClass
                    .methods
                    .singleOrNull { it.name == toolSpec.daemonEntryPoint.get() }
                    ?: error("Couldn't find daemon entry point '${toolSpec.daemonEntryPoint.get()}'")

                metricsReporter.measure(RUN_ENTRY_POINT) {
                    if (toolSpec.collectNativeCompilerMetrics.get()) {
                        val reportFile = Files.createTempFile("native_compiler_${::runInProcess.name}_report", ".json")
                        val toolArgsWithPerformance = toolArgs.toMutableList()
                        toolArgsWithPerformance.add("$dumpPerfArgument=${reportFile.toAbsolutePath()}")
                        entryPoint.invoke(null, toolArgsWithPerformance.toTypedArray())
                        metricsReporter.parseCompilerMetricsFromFile(reportFile.toFile())
                    } else {
                        entryPoint.invoke(null, toolArgs.toTypedArray())
                    }
                }
            } catch (t: InvocationTargetException) {
                throw t.targetException
            }
        }
    }

    private fun Properties.snapshot(): Properties = clone() as Properties

    private fun Sequence<Pair<String, String>>.escapeQuotesForWindows() =
        if (HostManager.hostIsMingw) map { (key, value) -> key.escapeQuotes() to value.escapeQuotes() } else this

    private fun String.escapeQuotes() = replace("\"", "\\\"")

    private fun Map<String, String>.toPrettyString(): String = buildString {
        append('[')
        if (this@toPrettyString.isNotEmpty()) append('\n')
        this@toPrettyString.entries.forEach { (key, value) ->
            append('\t').append(key).append(" = ").append(value.toPrettyString()).append('\n')
        }
        append(']')
    }

    private fun Collection<String>.toPrettyString(): String = buildString {
        append('[')
        if (this@toPrettyString.isNotEmpty()) append('\n')
        this@toPrettyString.forEach { append('\t').append(it.toPrettyString()).append('\n') }
        append(']')
    }

    private fun String.toPrettyString(): String =
        when {
            isEmpty() -> "\"\""
            any { it == '"' || it.isWhitespace() } -> '"' + escapeStringCharacters(this) + '"'
            else -> this
        }

    private fun ToolArguments.toArgFile(additionalArguments: List<String> = emptyList()): Path {
        val argFile = Files.createTempFile(
            "kotlinc-native-args",
            ".lst"
        )

        argFile.toFile().printWriter().use { w ->
            arguments.forEach { arg ->
                writeArgumentIntoWriter(arg, w)
            }
            additionalArguments.forEach { arg -> writeArgumentIntoWriter(arg, w) }
        }

        return argFile
    }

    private fun writeArgumentIntoWriter(arg: String, w: PrintWriter) {
        val escapedArg = arg
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        w.println("\"$escapedArg\"")
    }

    class ToolSpec(
        val displayName: Provider<String>,
        val optionalToolName: Provider<String>,
        val mainClass: Provider<String>,
        val daemonEntryPoint: Provider<String>,
        val classpath: FileCollection,
        val jvmArgs: ListProperty<String>,
        val shouldPassArgumentsViaArgFile: Provider<Boolean>,
        val collectNativeCompilerMetrics: Provider<Boolean>,
        val systemProperties: Map<String, String> = emptyMap(),
        val environment: Map<String, String> = emptyMap(),
        val environmentBlacklist: Set<String> = emptySet(),
    ) {
        val systemPropertiesBlacklist: Set<String> = setOf(
            "java.endorsed.dirs",       // Fix for KT-25887
            "user.dir",                 // Don't propagate the working dir of the current Gradle process
            "java.system.class.loader"  // Don't use custom class loaders
        )

        /**
         * Disable C2 compiler for HotSpot VM to improve compilation speed.
         */
        fun disableC2(): ToolSpec {
            System.getProperty("java.vm.name")?.let { vmName ->
                if (vmName.contains("HotSpot", true)) jvmArgs.add("-XX:TieredStopAtLevel=1")
            }

            return this
        }

        fun enableAssertions(): ToolSpec {
            jvmArgs.add("-ea")

            return this
        }

        fun configureDefaultMaxHeapSize(): ToolSpec {
            if (jvmArgs.get().none { it.startsWith("-Xmx") }) {
                jvmArgs.add("-Xmx3g")
            }

            return this
        }
    }

    data class ToolArguments(
        val shouldRunInProcessMode: Boolean,
        val compilerArgumentsLogLevel: KotlinCompilerArgumentsLogLevel,
        val arguments: List<String>,
    )

    /**
     * Flat, worker-safe representation of a single [runTool] invocation: the [ToolSpec]/[ToolArguments] that
     * belong to a [KotlinNativeToolRunner], plus the build services it needs, captured as plain values and
     * build-service references so Gradle can isolate (deep-copy) them when submitting [KotlinNativeToolWorkAction].
     */
    internal interface KotlinNativeToolWorkParameters : WorkParameters {
        // Runner configuration
        //
        // Note: there's no `metricsReporter` property here - unlike the other build-service references below,
        // a plain, Serializable BuildMetricsReporter would survive isolation silently as a disconnected copy
        // instead of failing loudly, so mutating it inside the worker would never reach the task's live reporter.
        // KotlinNativeToolWorkAction builds its own fresh reporter instead and reports back via [taskPath].
        val classLoadersCachingService: Property<ClassLoadersCachingBuildService>
        val fusService: Property<BuildFusService<out BuildFusService.Parameters>?>

        // ToolSpec
        val displayName: Property<String>
        val optionalToolName: Property<String>
        val mainClass: Property<String>
        val daemonEntryPoint: Property<String>
        val classpath: ConfigurableFileCollection
        val jvmArgs: ListProperty<String>
        val shouldPassArgumentsViaArgFile: Property<Boolean>
        val collectNativeCompilerMetrics: Property<Boolean>
        val systemProperties: MapProperty<String, String>
        val environment: MapProperty<String, String>
        val environmentBlacklist: SetProperty<String>

        // ToolArguments
        val shouldRunInProcessMode: Property<Boolean>
        val compilerArgumentsLogLevel: Property<KotlinCompilerArgumentsLogLevel>
        val arguments: ListProperty<String>

        // Task path to report collected metrics back to via TaskExecutionResults, if any - see runToolWithWorkers.
        val taskPath: Property<String>
    }

    /**
     * Runs a single [KotlinNativeToolRunner.runTool] invocation inside a Gradle worker, rebuilding the runner
     * from [KotlinNativeToolWorkParameters] since the original, live runner can't be passed through worker isolation.
     */
    internal abstract class KotlinNativeToolWorkAction @Inject constructor(
        private val objectFactory: ObjectFactory,
    ) : WorkAction<KotlinNativeToolWorkParameters> {
        override fun execute() {
            val metricsReporter = BuildMetricsReporterImpl<BuildTimeMetric, BuildPerformanceMetric>()
            val runner = objectFactory.newInstance<KotlinNativeToolRunner>(
                objectFactory.property<BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric>>(metricsReporter),
                parameters.classLoadersCachingService,
                parameters.toToolSpec(objectFactory),
                parameters.fusService,
            )
            try {
                runner.runTool(parameters.toToolArguments())
            } finally {
                parameters.taskPath.orNull?.let { taskPath ->
                    TaskExecutionResults[taskPath] = TaskExecutionResult(buildMetrics = metricsReporter.getMetrics())
                }
            }
        }
    }

    internal fun BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric>.parseCompilerMetricsFromFile(
        jsonFile: File,
    ) {
        if (!jsonFile.isFile()) return
        try {
            addCompilerMetrics(CompilerMetricsJson.parse(jsonFile.readText()))
        } catch (e: Exception) {
            errorMessageCollector.report(FusMetricRetrievalException("Failed to parse metrics from file ${jsonFile.absolutePath}", e), location = null)
        }
    }
}

private fun KotlinNativeToolRunner.KotlinNativeToolWorkParameters.toToolSpec(
    objectFactory: ObjectFactory,
): KotlinNativeToolRunner.ToolSpec {
    // TODO: Report this WorkParameters generic-erasure issue to Gradle if it is not tracked yet.
    // jvmArgs reaches us as Provider<List<Object>>, which breaks ToolSpec's internal
    // listProperty<String>().value(provider)-style checks if passed straight through.
    // We materialize it into a fresh, properly-typed ListProperty<String> instead - the same
    // workaround already used by CInteropWorkAction for the identical issue.
    val materializedJvmArgs = objectFactory.listProperty<String>().apply { set(jvmArgs.map { it }) }

    return KotlinNativeToolRunner.ToolSpec(
        displayName = displayName,
        optionalToolName = optionalToolName,
        mainClass = mainClass,
        daemonEntryPoint = daemonEntryPoint,
        classpath = classpath,
        jvmArgs = materializedJvmArgs,
        shouldPassArgumentsViaArgFile = shouldPassArgumentsViaArgFile,
        collectNativeCompilerMetrics = collectNativeCompilerMetrics,
        systemProperties = systemProperties.get(),
        environment = environment.get(),
        environmentBlacklist = environmentBlacklist.get(),
    )
}

private fun KotlinNativeToolRunner.KotlinNativeToolWorkParameters.toToolArguments(): KotlinNativeToolRunner.ToolArguments =
    KotlinNativeToolRunner.ToolArguments(
        shouldRunInProcessMode = shouldRunInProcessMode.get(),
        compilerArgumentsLogLevel = compilerArgumentsLogLevel.get(),
        arguments = arguments.get(),
    )
