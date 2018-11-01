/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.tasks.GradleMessageCollector
import org.jetbrains.kotlin.gradle.tasks.throwGradleExceptionIfError
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.io.Serializable
import java.net.URLClassLoader
import java.rmi.RemoteException
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
    val incrementalModuleInfo: IncrementalModuleInfo?
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

    private val log: KotlinLogger =
        SL4JKotlinLogger(LoggerFactory.getLogger("GradleKotlinCompilerWork"))
    private val messageCollector = GradleMessageCollector(log)

    private val isIncremental: Boolean
        get() = incrementalCompilationEnvironment != null

    override fun run() {
        val exitCode = compileWithDaemonOrFallbackImpl()
        throwGradleExceptionIfError(exitCode)
    }

    private fun compileWithDaemonOrFallbackImpl(): ExitCode {
        with(log) {
            kotlinDebug { "Kotlin compiler class: ${compilerClassName}" }
            kotlinDebug { "Kotlin compiler classpath: ${compilerFullClasspath.joinToString { it.canonicalPath }}" }
            kotlinDebug { "Kotlin compiler args: ${compilerArgs.joinToString(" ")}" }
        }

        val executionStrategy = kotlinCompilerExecutionStrategy()
        if (executionStrategy == DAEMON_EXECUTION_STRATEGY) {
            val daemonExitCode = compileWithDaemon()

            if (daemonExitCode != null) {
                return daemonExitCode
            } else {
                log.warn("Could not connect to kotlin daemon. Using fallback strategy.")
            }
        }

        val isGradleDaemonUsed = System.getProperty("org.gradle.daemon")?.let(String::toBoolean)
        return if (executionStrategy == IN_PROCESS_EXECUTION_STRATEGY || isGradleDaemonUsed == false) {
            compileInProcess()
        } else {
            compileOutOfProcess()
        }
    }

    private fun compileWithDaemon(): ExitCode? {
        val connection =
            try {
                GradleCompilerRunner.getDaemonConnectionImpl(
                    clientIsAliveFlagFile,
                    sessionFlagFile,
                    compilerFullClasspath,
                    messageCollector,
                    log.isDebugEnabled
                )
            } catch (e: Throwable) {
                log.warn("Caught an exception trying to connect to Kotlin Daemon")
                e.printStackTrace()
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
        val exitCode = try {
            val res = if (isIncremental) {
                incrementalCompilationWithDaemon(daemon, sessionId, targetPlatform)
            } else {
                nonIncrementalCompilationWithDaemon(daemon, sessionId, targetPlatform)
            }
            exitCodeFromProcessExitCode(log, res.get())
        } catch (e: Throwable) {
            log.warn("Compilation with Kotlin compile daemon was not successful")
            e.printStackTrace()
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
        targetPlatform: CompileService.TargetPlatform
    ): CompileService.CallResult<Int> {
        val compilationOptions = CompilationOptions(
            compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
            targetPlatform = targetPlatform,
            reportCategories = reportCategories(isVerbose),
            reportSeverity = reportSeverity(isVerbose),
            requestedCompilationResults = emptyArray()
        )
        val servicesFacade = GradleCompilerServicesFacadeImpl(log, messageCollector)
        return daemon.compile(sessionId, compilerArgs, compilationOptions, servicesFacade, compilationResults = null)
    }

    private fun incrementalCompilationWithDaemon(
        daemon: CompileService,
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform
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
            localStateDirs = icEnv.localStateDirs,
            multiModuleICSettings = icEnv.multiModuleICSettings,
            modulesInfo = incrementalModuleInfo!!
        )

        log.info("Options for KOTLIN DAEMON: $compilationOptions")
        val servicesFacade = GradleIncrementalCompilerServicesFacadeImpl(log, messageCollector)
        val compilationResults = GradleCompilationResults(log, projectRootFile)
        return daemon.compile(sessionId, compilerArgs, compilationOptions, servicesFacade, compilationResults)
    }

    private fun compileOutOfProcess(): ExitCode =
        runToolInSeparateProcess(compilerArgs, compilerClassName, compilerFullClasspath, log, loggingMessageCollector)

    private fun compileInProcess(): ExitCode {
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

    // used only for process launching so far, but implements unused proper contract
    private val loggingMessageCollector: MessageCollector by lazy {
        object : MessageCollector {
            private var hasErrors = false
            private val messageRenderer = MessageRenderer.PLAIN_FULL_PATHS

            override fun clear() {
                hasErrors = false
            }

            override fun hasErrors(): Boolean = hasErrors

            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
                val locMessage = messageRenderer.render(severity, message, location)
                when (severity) {
                    CompilerMessageSeverity.EXCEPTION -> log.error(locMessage)
                    CompilerMessageSeverity.ERROR,
                    CompilerMessageSeverity.STRONG_WARNING,
                    CompilerMessageSeverity.WARNING,
                    CompilerMessageSeverity.INFO -> log.info(locMessage)
                    CompilerMessageSeverity.LOGGING -> log.debug(locMessage)
                    CompilerMessageSeverity.OUTPUT -> {
                    }
                }
            }
        }
    }
}