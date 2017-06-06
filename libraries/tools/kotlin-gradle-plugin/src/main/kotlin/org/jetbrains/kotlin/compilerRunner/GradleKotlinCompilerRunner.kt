/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.launchProcessWithFallback
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.gradle.plugin.ParentLastURLClassLoader
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.incremental.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.concurrent.thread

internal const val KOTLIN_COMPILER_EXECUTION_STRATEGY_PROPERTY = "kotlin.compiler.execution.strategy"
internal const val DAEMON_EXECUTION_STRATEGY = "daemon"
internal const val IN_PROCESS_EXECUTION_STRATEGY = "in-process"
internal const val OUT_OF_PROCESS_EXECUTION_STRATEGY = "out-of-process"
const val CREATED_CLIENT_FILE_PREFIX = "Created client-is-alive flag file: "
const val EXISTING_CLIENT_FILE_PREFIX = "Existing client-is-alive flag file: "
const val CREATED_SESSION_FILE_PREFIX = "Created session-is-alive flag file: "
const val EXISTING_SESSION_FILE_PREFIX = "Existing session-is-alive flag file: "
const val DELETED_SESSION_FILE_PREFIX = "Deleted session-is-alive flag file: "
const val COULD_NOT_CONNECT_TO_DAEMON_MESSAGE = "Could not connect to Kotlin compile daemon"

internal class GradleCompilerRunner(private val project: Project) : KotlinCompilerRunner<GradleCompilerEnvironment>() {
    override val log = GradleKotlinLogger(project.logger)

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

    fun runJvmCompiler(
            sourcesToCompile: List<File>,
            javaSourceRoots: Iterable<File>,
            args: K2JVMCompilerArguments,
            environment: GradleCompilerEnvironment
    ): ExitCode {
        val moduleFile = makeModuleFile(
                args.moduleName,
                isTest = false,
                outputDir = args.destinationAsFile,
                sourcesToCompile = sourcesToCompile,
                javaSourceRoots = javaSourceRoots,
                classpath = args.classpathAsList,
                friendDirs = args.friendPaths?.map(::File) ?: emptyList())
        args.module = moduleFile.absolutePath

        if (environment !is GradleIncrementalCompilerEnvironment) {
            args.destination = null
        }

        var deleteModuleFile = true

        try {
            val res = runCompiler(K2JVM_COMPILER, args, environment)
            deleteModuleFile = (res == ExitCode.OK || System.getProperty("kotlin.compiler.leave.module.file.on.error") == null)
            return res
        }
        finally {
            if (deleteModuleFile) {
                moduleFile.delete()
            }
        }
    }

    fun runJsCompiler(
            kotlinSources: List<File>,
            args: K2JSCompilerArguments,
            environment: GradleCompilerEnvironment
    ): ExitCode {
        args.freeArgs.addAll(kotlinSources.map { it.absolutePath })
        return runCompiler(K2JS_COMPILER, args, environment)
    }

    fun runMetadataCompiler(
            kotlinSources: List<File>,
            args: K2MetadataCompilerArguments,
            environment: GradleCompilerEnvironment
    ): ExitCode {
        args.freeArgs.addAll(kotlinSources.map { it.absolutePath })
        return runCompiler(K2METADATA_COMPILER, args, environment)
    }

    override fun compileWithDaemonOrFallback(
            compilerClassName: String,
            compilerArgs: CommonCompilerArguments,
            environment: GradleCompilerEnvironment
    ): ExitCode {
        if (compilerArgs.version) {
            project.logger.lifecycle("Kotlin version " + loadCompilerVersion(environment.compilerJar) +
                    " (JRE " + System.getProperty("java.runtime.version") + ")")
            compilerArgs.version = false
        }

        val argsArray = ArgumentUtils.convertArgumentsToStringList(compilerArgs).toTypedArray()
        with (project.logger) {
            kotlinDebug { "Kotlin compiler class: $compilerClassName" }
            kotlinDebug { "Kotlin compiler classpath: ${environment.compilerClasspath.map { it.canonicalPath }.joinToString()}" }
            kotlinDebug { "Kotlin compiler args: ${argsArray.joinToString(" ")}" }
        }

        val executionStrategy = System.getProperty(KOTLIN_COMPILER_EXECUTION_STRATEGY_PROPERTY) ?: DAEMON_EXECUTION_STRATEGY
        if (executionStrategy == DAEMON_EXECUTION_STRATEGY) {
            val daemonExitCode = compileWithDaemon(compilerClassName, compilerArgs, environment)

            if (daemonExitCode != null) {
                return daemonExitCode
            }
            else {
                log.warn("Could not connect to kotlin daemon. Using fallback strategy.")
            }
        }

        val isGradleDaemonUsed = System.getProperty("org.gradle.daemon")?.let(String::toBoolean)
        return if (executionStrategy == IN_PROCESS_EXECUTION_STRATEGY || isGradleDaemonUsed == false) {
            compileInProcess(argsArray, compilerClassName, environment)
        }
        else {
            compileOutOfProcess(argsArray, compilerClassName, environment)
        }
    }

    override fun compileWithDaemon(compilerClassName: String, compilerArgs: CommonCompilerArguments, environment: GradleCompilerEnvironment): ExitCode? {
        val connection =
                try {
                    getDaemonConnection(environment)
                }
                catch (e: Throwable) {
                    log.warn("Caught an exception trying to connect to Kotlin Daemon")
                    e.printStackTrace()
                    null
                }
        if (connection == null) {
            if (environment is GradleIncrementalCompilerEnvironment) {
                log.warn("Could not perform incremental compilation: $COULD_NOT_CONNECT_TO_DAEMON_MESSAGE")
            }
            else {
                log.warn(COULD_NOT_CONNECT_TO_DAEMON_MESSAGE)
            }
            return null
        }

        val (daemon, sessionId) = connection
        val exitCode = try {
            val res = if (environment is GradleIncrementalCompilerEnvironment) {
                incrementalCompilationWithDaemon(daemon, sessionId, environment)
            } else {
                nonIncrementalCompilationWithDaemon(daemon, sessionId, compilerClassName, environment)
            }
            exitCodeFromProcessExitCode(res.get())
        }
        catch (e: Throwable) {
            log.warn("Compilation with Kotlin compile daemon was not successful")
            e.printStackTrace()
            null
        }
        // todo: can we clear cache on the end of session?
        daemon.clearJarCache()
        logFinish(DAEMON_EXECUTION_STRATEGY)
        return exitCode
    }

    private fun nonIncrementalCompilationWithDaemon(
            daemon: CompileService,
            sessionId: Int,
            compilerClassName: String,
            environment: GradleCompilerEnvironment
    ): CompileService.CallResult<Int> {
        val targetPlatform = when (compilerClassName) {
            K2JVM_COMPILER -> CompileService.TargetPlatform.JVM
            K2JS_COMPILER -> CompileService.TargetPlatform.JS
            K2METADATA_COMPILER -> CompileService.TargetPlatform.METADATA
            else -> throw IllegalArgumentException("Unknown compiler type $compilerClassName")
        }

        val verbose = environment.compilerArgs.verbose
        val compilationOptions = CompilationOptions(
                compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                targetPlatform = targetPlatform,
                reportCategories = reportCategories(verbose),
                reportSeverity = reportSeverity(verbose),
                requestedCompilationResults = emptyArray())
        val servicesFacade = GradleCompilerServicesFacadeImpl(project, environment.messageCollector)
        val argsArray = ArgumentUtils.convertArgumentsToStringList(environment.compilerArgs).toTypedArray()
        return daemon.compile(sessionId, argsArray, compilationOptions, servicesFacade, compilationResults = null)
    }

    private fun incrementalCompilationWithDaemon(
            daemon: CompileService,
            sessionId: Int,
            environment: GradleIncrementalCompilerEnvironment
    ): CompileService.CallResult<Int> {
        val knownChangedFiles = environment.changedFiles as? ChangedFiles.Known

        val verbose = environment.compilerArgs.verbose
        val compilationOptions = IncrementalCompilationOptions(
                areFileChangesKnown = knownChangedFiles != null,
                modifiedFiles = knownChangedFiles?.modified,
                deletedFiles = knownChangedFiles?.removed,
                workingDir = environment.workingDir,
                customCacheVersion = GRADLE_CACHE_VERSION,
                customCacheVersionFileName = GRADLE_CACHE_VERSION_FILE_NAME,
                reportCategories = reportCategories(verbose),
                reportSeverity = reportSeverity(verbose),
                requestedCompilationResults = arrayOf(CompilationResultCategory.IC_COMPILE_ITERATION.code),
                compilerMode = CompilerMode.INCREMENTAL_COMPILER,
                targetPlatform = CompileService.TargetPlatform.JVM
        )
        val servicesFacade = GradleIncrementalCompilerServicesFacadeImpl(project, environment)
        val argsArray = ArgumentUtils.convertArgumentsToStringList(environment.compilerArgs).toTypedArray()
        return daemon.compile(sessionId, argsArray, compilationOptions, servicesFacade, GradleCompilationResults(project))
    }

    private fun reportCategories(verbose: Boolean): Array<Int> =
            if (!verbose) {
                arrayOf(ReportCategory.COMPILER_MESSAGE.code)
            }
            else {
                ReportCategory.values().map { it.code }.toTypedArray()
            }

    private fun reportSeverity(verbose: Boolean): Int =
            if (!verbose) {
                ReportSeverity.INFO.code
            }
            else {
                ReportSeverity.DEBUG.code
            }

    private fun compileOutOfProcess(
            argsArray: Array<String>,
            compilerClassName: String,
            environment: GradleCompilerEnvironment
    ): ExitCode {
        return runToolInSeparateProcess(argsArray, compilerClassName, environment.compilerClasspath, log, loggingMessageCollector)
    }

    private fun compileInProcess(
            argsArray: Array<String>,
            compilerClassName: String,
            environment: GradleCompilerEnvironment
    ): ExitCode {
        val stream = ByteArrayOutputStream()
        val out = PrintStream(stream)
        // todo: cache classloader?
        val classLoader = ParentLastURLClassLoader(environment.compilerClasspathURLs, this.javaClass.classLoader)
        val servicesClass = Class.forName(Services::class.java.canonicalName, true, classLoader)
        val emptyServices = servicesClass.getField("EMPTY").get(servicesClass)
        val compiler = Class.forName(compilerClassName, true, classLoader)

        val exec = compiler.getMethod(
                "execAndOutputXml",
                PrintStream::class.java,
                servicesClass,
                Array<String>::class.java
        )

        val res = exec.invoke(compiler.newInstance(), out, emptyServices, argsArray)
        val exitCode = ExitCode.valueOf(res.toString())
        processCompilerOutput(environment, stream, exitCode)
        logFinish(IN_PROCESS_EXECUTION_STRATEGY)
        return exitCode
    }

    private fun logFinish(strategy: String) = log.logFinish(strategy)

    override fun getDaemonConnection(environment: GradleCompilerEnvironment): CompileServiceSession? {
        val compilerId = CompilerId.makeCompilerId(environment.compilerClasspath)
        val clientIsAliveFlagFile = getOrCreateClientFlagFile(project)
        val sessionIsAliveFlagFile = getOrCreateSessionFlagFile(project)
        return newDaemonConnection(compilerId, clientIsAliveFlagFile, sessionIsAliveFlagFile, environment)
    }

    companion object {
        // created once per gradle instance
        // when gradle daemon dies, kotlin daemon should die too
        // however kotlin daemon (if it idles enough) can die before gradle daemon dies
        @Volatile
        private var clientIsAliveFlagFile: File? = null

        @Synchronized
        private fun getOrCreateClientFlagFile(project: Project): File {
            val log = project.logger
            if (clientIsAliveFlagFile == null || !clientIsAliveFlagFile!!.exists()) {
                val projectName = project.rootProject.name.normalizeForFlagFile()
                clientIsAliveFlagFile =  FileUtil.createTempFile("kotlin-compiler-in-$projectName-", ".alive", /*deleteOnExit =*/ true)
                log.kotlinDebug { CREATED_CLIENT_FILE_PREFIX + clientIsAliveFlagFile!!.canonicalPath }
            }
            else {
                log.kotlinDebug { EXISTING_CLIENT_FILE_PREFIX + clientIsAliveFlagFile!!.canonicalPath }
            }

            return clientIsAliveFlagFile!!
        }

        private fun String.normalizeForFlagFile(): String {
            val validChars = ('a'..'z') + ('0'..'9') + "-_"
            return filter { it in validChars }
        }

        // session is created per build
        @Volatile
        private var sessionFlagFile: File? = null

        // session files are deleted at org.jetbrains.kotlin.gradle.plugin.KotlinGradleBuildServices.buildFinished
        @Synchronized
        private fun getOrCreateSessionFlagFile(project: Project): File {
            val log = project.logger
            if (sessionFlagFile == null || !sessionFlagFile!!.exists()) {
                val sessionFilesDir = sessionsDir(project).apply { mkdirs() }
                sessionFlagFile = FileUtil.createTempFile(sessionFilesDir, "kotlin-compiler-", ".salive", /*deleteOnExit =*/ true)
                log.kotlinDebug { CREATED_SESSION_FILE_PREFIX + sessionFlagFile!!.relativeToRoot(project) }
            }
            else {
                log.kotlinDebug { EXISTING_SESSION_FILE_PREFIX + sessionFlagFile!!.relativeToRoot(project) }
            }

            return sessionFlagFile!!
        }

        internal fun sessionsDir(project: Project): File =
                File(File(project.rootProject.buildDir, "kotlin"), "sessions")
    }
}
