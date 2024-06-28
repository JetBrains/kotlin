/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.logging.*
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.internal.state.getTaskLogger
import org.jetbrains.kotlin.gradle.report.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.stackTraceAsString
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.IncrementalModuleInfo
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.io.*
import java.net.URLClassLoader
import java.rmi.RemoteException
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.inject.Inject

internal class ProjectFilesForCompilation(
    val projectRootFile: File,
    val clientIsAliveFlagFile: File,
    val sessionFlagFile: File,
    val buildDir: File
) : Serializable {
    constructor(
        logger: Logger,
        projectDir: File,
        buildDir: File,
        projectName: String,
        sessionDir: File
    ) : this(
        projectRootFile = projectDir,
        clientIsAliveFlagFile = GradleCompilerRunner.getOrCreateClientFlagFile(logger, projectName),
        sessionFlagFile = GradleCompilerRunner.getOrCreateSessionFlagFile(logger, sessionDir),
        buildDir = buildDir
    )

    companion object {
        const val serialVersionUID: Long = 0
    }
}

internal class GradleKotlinCompilerWorkArguments(
    val projectFiles: ProjectFilesForCompilation,
    val compilerFullClasspath: List<File>,
    val compilerClassName: String,
    val compilerArgs: Array<String>,
    val isVerbose: Boolean,
    val incrementalCompilationEnvironment: IncrementalCompilationEnvironment?,
    val incrementalModuleInfo: IncrementalModuleInfo?,
    val outputFiles: List<File>,
    val taskPath: String,
    val reportingSettings: ReportingSettings,
    val kotlinScriptExtensions: Array<String>,
    val allWarningsAsErrors: Boolean,
    val compilerExecutionSettings: CompilerExecutionSettings,
    val errorsFiles: Set<File>?,
    val kotlinPluginVersion: String,
    val kotlinLanguageVersion: KotlinVersion,
    val compilerArgumentsLogLevel: KotlinCompilerArgumentsLogLevel,
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 2L
    }
}

internal class GradleKotlinCompilerWork @Inject constructor(
    private val config: GradleKotlinCompilerWorkArguments
) : Runnable {

    private val metrics = if (config.reportingSettings.buildReportOutputs.isNotEmpty()) {
        BuildMetricsReporterImpl()
    } else {
        DoNothingBuildMetricsReporter
    }
    private var icLogLines: List<String> = emptyList()

    private val log: KotlinLogger = getTaskLogger(config.taskPath, null, "GradleKotlinCompilerWork")

    private val isIncremental: Boolean
        get() = config.incrementalCompilationEnvironment != null

    override fun run() {
        metrics.addTimeMetric(GradleBuildPerformanceMetric.START_WORKER_EXECUTION)
        metrics.startMeasure(GradleBuildTime.RUN_COMPILATION_IN_WORKER)
        try {
            val gradlePrintingMessageCollector = GradlePrintingMessageCollector(log, config.allWarningsAsErrors)
            val gradleMessageCollector = GradleErrorMessageCollector(
                log,
                gradlePrintingMessageCollector,
                kotlinPluginVersion = config.kotlinPluginVersion
            )
            val (exitCode, executionStrategy) = compileWithDaemonOrFallbackImpl(gradleMessageCollector, config.compilerArgumentsLogLevel)
            if (config.incrementalCompilationEnvironment?.disableMultiModuleIC == true) {
                config.incrementalCompilationEnvironment.multiModuleICSettings.buildHistoryFile.delete()
            }
            config.errorsFiles?.let {
                gradleMessageCollector.flush(it)
            }

            throwExceptionIfCompilationFailed(exitCode, executionStrategy)
        } finally {
            val taskInfo = TaskExecutionInfo(
                kotlinLanguageVersion = config.kotlinLanguageVersion,
                changedFiles = config.incrementalCompilationEnvironment?.changedFiles,
                compilerArguments = if (config.reportingSettings.includeCompilerArguments) config.compilerArgs else emptyArray(),
                tags = collectStatTags(),
            )
            metrics.endMeasure(GradleBuildTime.RUN_COMPILATION_IN_WORKER)
            val result = TaskExecutionResult(buildMetrics = metrics.getMetrics(), icLogLines = icLogLines, taskInfo = taskInfo)
            TaskExecutionResults[config.taskPath] = result
        }
    }

    private fun collectStatTags(): Set<StatTag> {
        val statTags = HashSet<StatTag>()
        config.incrementalCompilationEnvironment?.icFeatures?.withAbiSnapshot?.ifTrue { statTags.add(StatTag.ABI_SNAPSHOT) }
        if (config.incrementalCompilationEnvironment?.classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled) {
            statTags.add(StatTag.ARTIFACT_TRANSFORM)
        }
        return statTags
    }

    private fun compileWithDaemonOrFallbackImpl(
        messageCollector: MessageCollector,
        compilerArgsLogLevel: KotlinCompilerArgumentsLogLevel,
    ): Pair<ExitCode, KotlinCompilerExecutionStrategy> {
        with(log) {
            kotlinDebug { "Kotlin compiler class: ${config.compilerClassName}" }
            kotlinDebug {
                val compilerClasspath = config.compilerFullClasspath.joinToString(File.pathSeparator) { it.normalize().absolutePath }
                "Kotlin compiler classpath: $compilerClasspath"
            }
            logCompilerArgumentsMessage(compilerArgsLogLevel) {
                "${config.taskPath} Kotlin compiler args: ${config.compilerArgs.joinToString(" ")}"
            }
        }

        if (config.compilerExecutionSettings.strategy == KotlinCompilerExecutionStrategy.DAEMON) {
            try {
                return compileWithDaemon(messageCollector) to KotlinCompilerExecutionStrategy.DAEMON
            } catch (e: Throwable) {
                messageCollector.report(
                    severity = CompilerMessageSeverity.EXCEPTION,
                    message = "Daemon compilation failed: ${e.message}\n${e.stackTraceToString()}"
                )
                val recommendation = """
                    Try ./gradlew --stop if this issue persists
                    If it does not look related to your configuration, please file an issue with logs to https://kotl.in/issue
                """.trimIndent()
                if (!config.compilerExecutionSettings.useDaemonFallbackStrategy) {
                    throw RuntimeException(
                        """
                        |Failed to compile with Kotlin daemon.
                        |Fallback strategy (compiling without Kotlin daemon) is turned off.
                        |$recommendation
                        """.trimMargin(),
                        e
                    )
                }
                val failDetails = e.stackTraceAsString().removeSuffixIfPresent("\n")
                log.warn(
                    """
                    |Failed to compile with Kotlin daemon: $failDetails
                    |Using fallback strategy: Compile without Kotlin daemon
                    |$recommendation
                    """.trimMargin()
                )
            }
        }

        val isGradleDaemonUsed = System.getProperty("org.gradle.daemon")?.let(String::toBoolean)
        return if (config.compilerExecutionSettings.strategy == KotlinCompilerExecutionStrategy.IN_PROCESS ||
            isGradleDaemonUsed == false
        ) {
            compileInProcess(messageCollector) to KotlinCompilerExecutionStrategy.IN_PROCESS
        } else {
            compileOutOfProcess() to KotlinCompilerExecutionStrategy.OUT_OF_PROCESS
        }
    }

    private fun compileWithDaemon(messageCollector: MessageCollector): ExitCode {
        val isDebugEnabled = log.isDebugEnabled || System.getProperty("kotlin.daemon.debug.log")?.toBoolean() ?: true
        val daemonMessageCollector =
            if (isDebugEnabled) messageCollector else MessageCollector.NONE
        val connection =
            metrics.measure(GradleBuildTime.CONNECT_TO_DAEMON) {
                GradleCompilerRunner.getDaemonConnectionImpl(
                    config.projectFiles.clientIsAliveFlagFile,
                    config.projectFiles.sessionFlagFile,
                    config.compilerFullClasspath,
                    daemonMessageCollector,
                    isDebugEnabled = isDebugEnabled,
                    daemonJvmArgs = config.compilerExecutionSettings.daemonJvmArgs
                )
            } ?: throw RuntimeException(COULD_NOT_CONNECT_TO_DAEMON_MESSAGE) // TODO: Add root cause

        val (daemon, sessionId) = connection

        if (log.isDebugEnabled) {
            daemon.getDaemonJVMOptions().takeIf { it.isGood }?.let { jvmOpts ->
                log.debug("Kotlin compile daemon JVM options: ${jvmOpts.get().mappers.flatMap { it.toArgs("-") }}")
            }
        }

        val memoryUsageBeforeBuild = daemon.getUsedMemory(withGC = false).takeIf { it.isGood }?.get()

        val targetPlatform = when (config.compilerClassName) {
            KotlinCompilerClass.JVM -> CompileService.TargetPlatform.JVM
            KotlinCompilerClass.JS -> CompileService.TargetPlatform.JS
            KotlinCompilerClass.METADATA -> CompileService.TargetPlatform.METADATA
            else -> throw IllegalArgumentException("Unknown compiler type ${config.compilerClassName}")
        }
        val bufferingMessageCollector = GradleBufferingMessageCollector()
        val exitCode = try {
            val res = if (isIncremental) {
                incrementalCompilationWithDaemon(daemon, sessionId, targetPlatform, bufferingMessageCollector)
            } else {
                nonIncrementalCompilationWithDaemon(daemon, sessionId, targetPlatform, bufferingMessageCollector)
            }
            bufferingMessageCollector.flush(messageCollector)
            exitCodeFromProcessExitCode(log, res.get())
        } catch (e: Throwable) {
            bufferingMessageCollector.flush(messageCollector)
            wrapAndRethrowCompilationException(KotlinCompilerExecutionStrategy.DAEMON, e)
        } finally {
            val memoryUsageAfterBuild = runCatching { daemon.getUsedMemory(withGC = false).takeIf { it.isGood }?.get() }.getOrNull()

            if (memoryUsageAfterBuild == null || memoryUsageBeforeBuild == null) {
                log.debug("Unable to calculate memory usage")
            } else {
                metrics.addMetric(GradleBuildPerformanceMetric.DAEMON_INCREASED_MEMORY, memoryUsageAfterBuild - memoryUsageBeforeBuild)
                metrics.addMetric(GradleBuildPerformanceMetric.DAEMON_MEMORY_USAGE, memoryUsageAfterBuild)
            }


            // todo: can we clear cache on the end of session?
            // often source of the NoSuchObjectException and UnmarshalException, probably caused by the failed/crashed/exited daemon
            // TODO: implement a proper logic to avoid remote calls in such cases
            try {
                metrics.measure(GradleBuildTime.CLEAR_JAR_CACHE) {
                    // releasing compile session implies clearing the jar cache
                    daemon.releaseCompileSession(sessionId)
                }
            } catch (e: RemoteException) {
                log.warn("Unable to release compile session, maybe daemon is already down: $e")
            }
        }
        log.logFinish(KotlinCompilerExecutionStrategy.DAEMON)
        return exitCode
    }

    private fun nonIncrementalCompilationWithDaemon(
        daemon: CompileService,
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        bufferingMessageCollector: GradleBufferingMessageCollector
    ): CompileService.CallResult<Int> {
        metrics.addAttribute(BuildAttribute.IC_IS_NOT_ENABLED)
        val compilationOptions = CompilationOptions(
            compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
            targetPlatform = targetPlatform,
            reportCategories = reportCategories(config.isVerbose),
            reportSeverity = reportSeverity(config.isVerbose),
            requestedCompilationResults = emptyArray(),
            kotlinScriptExtensions = config.kotlinScriptExtensions
        )
        val servicesFacade = GradleCompilerServicesFacadeImpl(log, bufferingMessageCollector)
        val compilationResults = GradleCompilationResults(log, config.projectFiles.projectRootFile)
        return metrics.measure(GradleBuildTime.NON_INCREMENTAL_COMPILATION_DAEMON) {
            daemon.compile(sessionId, config.compilerArgs, compilationOptions, servicesFacade, compilationResults)
        }.also {
            metrics.addMetrics(compilationResults.buildMetrics)
            icLogLines = compilationResults.icLogLines
        }
    }

    private fun incrementalCompilationWithDaemon(
        daemon: CompileService,
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        bufferingMessageCollector: GradleBufferingMessageCollector
    ): CompileService.CallResult<Int> {
        val icEnv = config.incrementalCompilationEnvironment ?: error("incrementalCompilationEnvironment is null!")
        val knownChangedFiles = icEnv.changedFiles as? SourcesChanges.Known
        val requestedCompilationResults = requestedCompilationResults()
        val compilationOptions = IncrementalCompilationOptions(
            areFileChangesKnown = knownChangedFiles != null,
            modifiedFiles = knownChangedFiles?.modifiedFiles,
            deletedFiles = knownChangedFiles?.removedFiles,
            classpathChanges = icEnv.classpathChanges,
            workingDir = icEnv.workingDir,
            reportCategories = reportCategories(config.isVerbose),
            reportSeverity = reportSeverity(config.isVerbose),
            requestedCompilationResults = requestedCompilationResults.map { it.code }.toTypedArray(),
            compilerMode = CompilerMode.INCREMENTAL_COMPILER,
            targetPlatform = targetPlatform,
            usePreciseJavaTracking = icEnv.usePreciseJavaTracking,
            outputFiles = config.outputFiles,
            multiModuleICSettings = icEnv.multiModuleICSettings,
            modulesInfo = config.incrementalModuleInfo,
            rootProjectDir = icEnv.rootProjectDir,
            buildDir = icEnv.buildDir,
            kotlinScriptExtensions = config.kotlinScriptExtensions,
            icFeatures = icEnv.icFeatures,
        )

        log.info("Options for KOTLIN DAEMON: $compilationOptions")
        val servicesFacade = GradleIncrementalCompilerServicesFacadeImpl(log, bufferingMessageCollector)
        val compilationResults = GradleCompilationResults(log, config.projectFiles.projectRootFile)
        metrics.addTimeMetric(GradleBuildPerformanceMetric.CALL_KOTLIN_DAEMON)
        return metrics.measure(GradleBuildTime.RUN_COMPILATION) {
            daemon.compile(sessionId, config.compilerArgs, compilationOptions, servicesFacade, compilationResults)
        }.also {
            metrics.addMetrics(compilationResults.buildMetrics)
            icLogLines = compilationResults.icLogLines
        }
    }

    private fun compileOutOfProcess(): ExitCode {
        metrics.addAttribute(BuildAttribute.OUT_OF_PROCESS_EXECUTION)
        cleanOutputsAndLocalState(config.outputFiles, log, metrics, reason = "out-of-process execution strategy is non-incremental")

        return metrics.measure(GradleBuildTime.NON_INCREMENTAL_COMPILATION_OUT_OF_PROCESS) {
            runToolInSeparateProcess(
                config.compilerArgs,
                config.compilerClassName,
                config.compilerFullClasspath,
                log,
                config.projectFiles.buildDir,
            )
        }
    }

    private fun compileInProcess(messageCollector: MessageCollector): ExitCode {
        metrics.addAttribute(BuildAttribute.IN_PROCESS_EXECUTION)
        cleanOutputsAndLocalState(config.outputFiles, log, metrics, reason = "in-process execution strategy is non-incremental")

        metrics.startMeasure(GradleBuildTime.NON_INCREMENTAL_COMPILATION_IN_PROCESS)
        // in-process compiler should always be run in a different thread
        // to avoid leaking thread locals from compiler (see KT-28037)
        val threadPool = Executors.newSingleThreadExecutor()
        val bufferingMessageCollector = GradleBufferingMessageCollector()
        return try {
            val future = threadPool.submit(Callable {
                compileInProcessImpl(bufferingMessageCollector)
            })
            future.get()
        } finally {
            bufferingMessageCollector.flush(messageCollector)
            threadPool.shutdown()

            metrics.endMeasure(GradleBuildTime.NON_INCREMENTAL_COMPILATION_IN_PROCESS)
        }
    }

    private fun compileInProcessImpl(messageCollector: MessageCollector): ExitCode {
        val stream = ByteArrayOutputStream()
        val out = PrintStream(stream)
        // todo: cache classloader?
        val classLoader = URLClassLoader(config.compilerFullClasspath.map { it.toURI().toURL() }.toTypedArray())
        val servicesClass = Class.forName(Services::class.java.canonicalName, true, classLoader)
        val emptyServices = servicesClass.getField("EMPTY").get(servicesClass)
        val compiler = Class.forName(config.compilerClassName, true, classLoader)

        val exec = compiler.getMethod(
            "execAndOutputXml",
            PrintStream::class.java,
            servicesClass,
            Array<String>::class.java
        )

        val res = exec.invoke(compiler.declaredConstructors.single().newInstance(), out, emptyServices, config.compilerArgs)
        val exitCode = ExitCode.valueOf(res.toString())
        processCompilerOutput(
            messageCollector,
            OutputItemsCollectorImpl(),
            stream,
            exitCode
        )
        try {
            metrics.measure(GradleBuildTime.CLEAR_JAR_CACHE) {
                val coreEnvironment = Class.forName("org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment", true, classLoader)
                val dispose = coreEnvironment.getMethod("disposeApplicationEnvironment")
                dispose.invoke(null)
            }
        } catch (e: Throwable) {
            log.warn("Unable to clear jar cache after in-process compilation: $e")
        }
        log.logFinish(KotlinCompilerExecutionStrategy.IN_PROCESS)
        return exitCode
    }

    private fun requestedCompilationResults(): EnumSet<CompilationResultCategory> {
        val requestedCompilationResults = EnumSet.of(CompilationResultCategory.IC_COMPILE_ITERATION)
        when (config.reportingSettings.buildReportMode) {
            BuildReportMode.NONE -> null
            BuildReportMode.SIMPLE -> CompilationResultCategory.BUILD_REPORT_LINES
            BuildReportMode.VERBOSE -> CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES
        }?.let { requestedCompilationResults.add(it) }
        if (config.reportingSettings.buildReportOutputs.isNotEmpty()) {
            requestedCompilationResults.add(CompilationResultCategory.BUILD_METRICS)
        }
        return requestedCompilationResults
    }

    private fun reportCategories(verbose: Boolean): Array<Int> =
        if (!verbose) {
            arrayOf(ReportCategory.COMPILER_MESSAGE.code, ReportCategory.IC_MESSAGE.code)
        } else {
            ReportCategory.values().map { it.code }.toTypedArray()
        }

    private fun reportSeverity(verbose: Boolean): Int =
        if (!verbose) {
            ReportSeverity.INFO.code
        } else {
            ReportSeverity.DEBUG.code
        }
}