/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.gradle.logging.*
import org.jetbrains.kotlin.gradle.tasks.clearLocalStateDirectories
import org.jetbrains.kotlin.gradle.tasks.throwGradleExceptionIfError
import org.jetbrains.kotlin.gradle.utils.stackTraceAsString
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.DELETE_MODULE_FILE_PROPERTY
import org.slf4j.LoggerFactory
import java.io.*
import java.net.URLClassLoader
import java.rmi.RemoteException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.inject.Inject

internal class ProjectFilesForCompilation(
    val projectRootFile: File,
    val clientIsAliveFlagFile: File,
    val sessionFlagFile: File
) : Serializable {
    constructor(project: Project) : this(
        projectRootFile = project.rootProject.projectDir,
        clientIsAliveFlagFile = GradleCompilerRunner.getOrCreateClientFlagFile(project),
        sessionFlagFile = GradleCompilerRunner.getOrCreateSessionFlagFile(project)
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
    val buildFile: File?,
    val localStateDirectories: List<File>,
    val taskPath: String
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}

internal class GradleKotlinCompilerWork @Inject constructor(
    /**
     * Arguments are passed through [GradleKotlinCompilerWorkArguments],
     * because Gradle Workers API does not support nullable arguments (https://github.com/gradle/gradle/issues/2405),
     * and because Workers API does not support named arguments,
     * which are useful when there are many arguments with the same type
     * (to protect against parameters reordering bugs)
     */
    config: GradleKotlinCompilerWorkArguments
) : Runnable {
    private val projectRootFile = config.projectFiles.projectRootFile
    private val clientIsAliveFlagFile = config.projectFiles.clientIsAliveFlagFile
    private val sessionFlagFile = config.projectFiles.sessionFlagFile
    private val compilerFullClasspath = config.compilerFullClasspath
    private val compilerClassName = config.compilerClassName
    private val compilerArgs = config.compilerArgs
    private val isVerbose = config.isVerbose
    private val incrementalCompilationEnvironment = config.incrementalCompilationEnvironment
    private val incrementalModuleInfo = config.incrementalModuleInfo
    private val buildFile = config.buildFile
    private val localStateDirectories = config.localStateDirectories
    private val taskPath = config.taskPath

    private val log: KotlinLogger =
        TaskLoggers.get(taskPath)?.let { GradleKotlinLogger(it) }
            ?: run {
                System.err.println("Could not get logger for '$taskPath'. Falling back to sl4j logger")
                SL4JKotlinLogger(LoggerFactory.getLogger("GradleKotlinCompilerWork"))
            }

    private val isIncremental: Boolean
        get() = incrementalCompilationEnvironment != null

    override fun run() {
        if (!isIncremental) {
            clearLocalStateDirectories(log, localStateDirectories, "IC is disabled")
        }

        val messageCollector = GradlePrintingMessageCollector(log)
        val exitCode = try {
            compileWithDaemonOrFallbackImpl(messageCollector)
        } catch (e: Throwable) {
            clearLocalStateDirectories(log, localStateDirectories, "exception when running compiler")
            throw e
        } finally {
            if (buildFile != null && System.getProperty(DELETE_MODULE_FILE_PROPERTY) != "false") {
                buildFile.delete()
            }
        }

        if (incrementalCompilationEnvironment != null) {
            if (incrementalCompilationEnvironment.disableMultiModuleIC) {
                incrementalCompilationEnvironment.multiModuleICSettings.buildHistoryFile.delete()
            }

            if (exitCode != ExitCode.OK) {
                clearLocalStateDirectories(log, localStateDirectories, "exit code: $exitCode")
            }
        }

        throwGradleExceptionIfError(exitCode)
    }

    private fun compileWithDaemonOrFallbackImpl(messageCollector: MessageCollector): ExitCode {
        with(log) {
            kotlinDebug { "Kotlin compiler class: ${compilerClassName}" }
            kotlinDebug { "Kotlin compiler classpath: ${compilerFullClasspath.joinToString { it.canonicalPath }}" }
            kotlinDebug { "Kotlin compiler args: ${compilerArgs.joinToString(" ")}" }
        }

        val executionStrategy = kotlinCompilerExecutionStrategy()
        if (executionStrategy == DAEMON_EXECUTION_STRATEGY) {
            val daemonExitCode = compileWithDaemon(messageCollector)

            if (daemonExitCode != null) {
                return daemonExitCode
            } else {
                log.warn("Could not connect to kotlin daemon. Using fallback strategy.")
            }
        }

        val isGradleDaemonUsed = System.getProperty("org.gradle.daemon")?.let(String::toBoolean)
        return if (executionStrategy == IN_PROCESS_EXECUTION_STRATEGY || isGradleDaemonUsed == false) {
            compileInProcess(messageCollector)
        } else {
            compileOutOfProcess()
        }
    }

    private fun compileWithDaemon(messageCollector: MessageCollector): ExitCode? {
        val isDebugEnabled = log.isDebugEnabled || System.getProperty("kotlin.daemon.debug.log")?.toBoolean() ?: false
        val daemonMessageCollector =
            if (isDebugEnabled) messageCollector else MessageCollector.NONE

        val connection =
            try {
                GradleCompilerRunner.getDaemonConnectionImpl(
                    clientIsAliveFlagFile,
                    sessionFlagFile,
                    compilerFullClasspath,
                    daemonMessageCollector,
                    isDebugEnabled
                )
            } catch (e: Throwable) {
                log.error("Caught an exception trying to connect to Kotlin Daemon:")
                log.error(e.stackTraceAsString())
                null
            }
        if (connection == null) {
            if (isIncremental) {
                log.warn("Could not perform incremental compilation: $COULD_NOT_CONNECT_TO_DAEMON_MESSAGE")
            } else {
                log.warn(COULD_NOT_CONNECT_TO_DAEMON_MESSAGE)
            }
            return null
        }

        val (daemon, sessionId) = connection
        val targetPlatform = when (compilerClassName) {
            KotlinCompilerClass.JVM -> CompileService.TargetPlatform.JVM
            KotlinCompilerClass.JS -> CompileService.TargetPlatform.JS
            KotlinCompilerClass.METADATA -> CompileService.TargetPlatform.METADATA
            else -> throw IllegalArgumentException("Unknown compiler type $compilerClassName")
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
            log.error("Compilation with Kotlin compile daemon was not successful")
            log.error(e.stackTraceAsString())
            null
        }
        // todo: can we clear cache on the end of session?
        // often source of the NoSuchObjectException and UnmarshalException, probably caused by the failed/crashed/exited daemon
        // TODO: implement a proper logic to avoid remote calls in such cases
        try {
            daemon.clearJarCache()
        } catch (e: RemoteException) {
            log.warn("Unable to clear jar cache after compilation, maybe daemon is already down: $e")
        }
        log.logFinish(DAEMON_EXECUTION_STRATEGY)
        return exitCode
    }

    private fun nonIncrementalCompilationWithDaemon(
        daemon: CompileService,
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        bufferingMessageCollector: GradleBufferingMessageCollector
    ): CompileService.CallResult<Int> {
        val compilationOptions = CompilationOptions(
            compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
            targetPlatform = targetPlatform,
            reportCategories = reportCategories(isVerbose),
            reportSeverity = reportSeverity(isVerbose),
            requestedCompilationResults = emptyArray()
        )
        val servicesFacade = GradleCompilerServicesFacadeImpl(log, bufferingMessageCollector)
        return daemon.compile(sessionId, compilerArgs, compilationOptions, servicesFacade, compilationResults = null)
    }

    private fun incrementalCompilationWithDaemon(
        daemon: CompileService,
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        bufferingMessageCollector: GradleBufferingMessageCollector
    ): CompileService.CallResult<Int> {
        val icEnv = incrementalCompilationEnvironment ?: error("incrementalCompilationEnvironment is null!")
        val knownChangedFiles = icEnv.changedFiles as? ChangedFiles.Known

        val compilationOptions = IncrementalCompilationOptions(
            areFileChangesKnown = knownChangedFiles != null,
            modifiedFiles = knownChangedFiles?.modified,
            deletedFiles = knownChangedFiles?.removed,
            workingDir = icEnv.workingDir,
            reportCategories = reportCategories(isVerbose),
            reportSeverity = reportSeverity(isVerbose),
            requestedCompilationResults = arrayOf(CompilationResultCategory.IC_COMPILE_ITERATION.code),
            compilerMode = CompilerMode.INCREMENTAL_COMPILER,
            targetPlatform = targetPlatform,
            usePreciseJavaTracking = icEnv.usePreciseJavaTracking,
            localStateDirs = localStateDirectories,
            multiModuleICSettings = icEnv.multiModuleICSettings,
            modulesInfo = incrementalModuleInfo!!
        )

        log.info("Options for KOTLIN DAEMON: $compilationOptions")
        val servicesFacade = GradleIncrementalCompilerServicesFacadeImpl(log, bufferingMessageCollector)
        val compilationResults = GradleCompilationResults(log, projectRootFile)
        return daemon.compile(sessionId, compilerArgs, compilationOptions, servicesFacade, compilationResults)
    }

    private fun compileOutOfProcess(): ExitCode =
        runToolInSeparateProcess(compilerArgs, compilerClassName, compilerFullClasspath, log)

    private fun compileInProcess(messageCollector: MessageCollector): ExitCode {
        // in-process compiler should always be run in a different thread
        // to avoid leaking thread locals from compiler (see KT-28037)
        val threadPool = Executors.newSingleThreadExecutor()
        val bufferingMessageCollector = GradleBufferingMessageCollector()
        try {
            val future = threadPool.submit(Callable {
                compileInProcessImpl(bufferingMessageCollector)
            })
            return future.get()
        } finally {
            bufferingMessageCollector.flush(messageCollector)
            threadPool.shutdown()
        }
    }

    private fun compileInProcessImpl(messageCollector: MessageCollector): ExitCode {
        val stream = ByteArrayOutputStream()
        val out = PrintStream(stream)
        // todo: cache classloader?
        val classLoader = URLClassLoader(compilerFullClasspath.map { it.toURI().toURL() }.toTypedArray())
        val servicesClass = Class.forName(Services::class.java.canonicalName, true, classLoader)
        val emptyServices = servicesClass.getField("EMPTY").get(servicesClass)
        val compiler = Class.forName(compilerClassName, true, classLoader)

        val exec = compiler.getMethod(
            "execAndOutputXml",
            PrintStream::class.java,
            servicesClass,
            Array<String>::class.java
        )

        val res = exec.invoke(compiler.newInstance(), out, emptyServices, compilerArgs)
        val exitCode = ExitCode.valueOf(res.toString())
        processCompilerOutput(
            messageCollector,
            OutputItemsCollectorImpl(),
            stream,
            exitCode
        )
        log.logFinish(IN_PROCESS_EXECUTION_STRATEGY)
        return exitCode
    }

    private fun reportCategories(verbose: Boolean): Array<Int> =
        if (!verbose) {
            arrayOf(ReportCategory.COMPILER_MESSAGE.code)
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