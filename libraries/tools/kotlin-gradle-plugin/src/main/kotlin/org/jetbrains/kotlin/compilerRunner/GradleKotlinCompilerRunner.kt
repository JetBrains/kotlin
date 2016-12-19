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

import net.rubygrapefruit.platform.Native
import net.rubygrapefruit.platform.ProcessLauncher
import org.gradle.api.Project
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.RemoteOutputStreamServer
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationServicesFacade
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.gradle.plugin.ParentLastURLClassLoader
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.gradle.tasks.SourceRoots
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile
import org.jetbrains.kotlin.incremental.makeModuleFile
import java.io.*
import java.rmi.server.UnicastRemoteObject
import kotlin.concurrent.thread

internal const val KOTLIN_COMPILER_EXECUTION_STRATEGY_PROPERTY = "kotlin.compiler.execution.strategy"
internal const val DAEMON_EXECUTION_STRATEGY = "daemon"
internal const val IN_PROCESS_EXECUTION_STRATEGY = "in-process"
internal const val OUT_OF_PROCESS_EXECUTION_STRATEGY = "out-of-process"

internal class GradleCompilerRunner(private val project: Project) : KotlinCompilerRunner<GradleCompilerEnvironment>() {
    override val log = GradleKotlinLogger(project.logger)
    private val flagFile = run {
        val dir = File(project.rootProject.buildDir, "kotlin").apply { mkdirs() }
        File(dir, "daemon-is-alive").apply {
            if (!exists()) {
                createNewFile()
                deleteOnExit()
            }
        }
    }

    fun runJvmCompiler(
            sourcesToCompile: List<File>,
            javaSourceRoots: Iterable<File>,
            args: K2JVMCompilerArguments,
            environment: GradleCompilerEnvironment
    ): ExitCode {
        val outputDir = args.destinationAsFile

        if (environment !is GradleIncrementalCompilerEnvironment) {
            log.debug("Removing all kotlin classes in $outputDir")
            // we're free to delete all classes since only we know about that directory
            outputDir.deleteRecursively()
        }

        val moduleFile = makeModuleFile(
                args.moduleName,
                isTest = false,
                outputDir = outputDir,
                sourcesToCompile = sourcesToCompile,
                javaSourceRoots = javaSourceRoots,
                classpath = args.classpathAsList,
                friendDirs = args.friendPaths?.map(::File) ?: emptyList())
        args.module = moduleFile.absolutePath

        val additionalArguments = ""

        try {
            return runCompiler(K2JVM_COMPILER, args, additionalArguments, environment)
        }
        finally {
            moduleFile.delete()
        }
    }

    fun runJsCompiler(
            kotlinSources: List<File>,
            args: K2JSCompilerArguments,
            environment: GradleCompilerEnvironment
    ): ExitCode {
        val additionalArguments = kotlinSources.joinToString(separator = " ") { it.absolutePath }
        return runCompiler(K2JS_COMPILER, args, additionalArguments, environment)
    }

    override fun doRunCompiler(compilerClassName: String, argsArray: Array<String>, environment: GradleCompilerEnvironment): ExitCode {
        with (project.logger) {
            kotlinDebug { "Kotlin compiler class: $compilerClassName" }
            kotlinDebug { "Kotlin compiler classpath: ${environment.compilerClasspath.map { it.canonicalPath }.joinToString()}" }
            kotlinDebug { "Kotlin compiler args: ${argsArray.joinToString(" ")}" }
        }

        val executionStrategy = System.getProperty(KOTLIN_COMPILER_EXECUTION_STRATEGY_PROPERTY) ?: DAEMON_EXECUTION_STRATEGY
        if (executionStrategy == DAEMON_EXECUTION_STRATEGY) {
            val daemonExitCode = compileWithDaemon(compilerClassName, argsArray, environment)

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

    override fun compileWithDaemon(compilerClassName: String, argsArray: Array<String>, environment: GradleCompilerEnvironment): ExitCode? {
        val exitCode = if (environment is GradleIncrementalCompilerEnvironment) {
            performIncrementalCompilationWithDaemon(argsArray, environment)
        }
        else {
            super.compileWithDaemon(compilerClassName, argsArray, environment)
        }
        exitCode?.let {
            withDaemon(environment, retryOnConnectionError = true) { daemon, sessionId ->
                daemon.clearJarCache()
            }
            logFinish(DAEMON_EXECUTION_STRATEGY)
        }
        return exitCode
    }

    private fun performIncrementalCompilationWithDaemon(argsArray: Array<String>, environment: GradleIncrementalCompilerEnvironment): ExitCode? {
        val port = SOCKET_ANY_FREE_PORT
        val services = IncrementalCompilationFacadeImpl(environment, port)
        val compilerOut = ByteArrayOutputStream()
        val daemonOut = ByteArrayOutputStream()

        val res = withDaemon(environment, retryOnConnectionError = true) { daemon, sessionId ->
            val remoteCompilerOut = RemoteOutputStreamServer(compilerOut, port)
            val remoteDaemonOut = RemoteOutputStreamServer(daemonOut, port)
            daemon.serverSideJvmIC(sessionId, argsArray, services, remoteCompilerOut, remoteDaemonOut, null)
        }

        val exitCode = res?.get()?.let { exitCodeFromProcessExitCode(it) }
        if (exitCode == null) {
            log.warn("Could not perform incremental compilation with Kotlin compile daemon, " +
                    "non-incremental compilation in fallback mode will be performed")
        }
        processCompilerOutput(environment, compilerOut, exitCode)
        BufferedReader(StringReader(daemonOut.toString())).forEachLine {
            environment.messageCollector.report(CompilerMessageSeverity.INFO, it, CompilerMessageLocation.NO_LOCATION)
        }
        return exitCode
    }

    private fun compileOutOfProcess(
            argsArray: Array<String>,
            compilerClassName: String,
            environment: GradleCompilerEnvironment
    ): ExitCode {
        val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
        val classpathString = environment.compilerClasspath.map {it.absolutePath}.joinToString(separator = File.pathSeparator)
        val builder = ProcessBuilder(javaBin, "-cp", classpathString, compilerClassName, *argsArray)
        val processLauncher = Native.get(ProcessLauncher::class.java)
        val process = processLauncher.start(builder)

        // important to read inputStream, otherwise the process may hang on some systems
        val readErrThread = thread {
            process.errorStream!!.bufferedReader().forEachLine {
                System.err.println(it)
            }
        }
        process.inputStream!!.bufferedReader().forEachLine {
            System.out.println(it)
        }
        readErrThread.join()

        val exitCode = process.waitFor()
        logFinish(OUT_OF_PROCESS_EXECUTION_STRATEGY)
        return exitCodeFromProcessExitCode(exitCode)
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

    private fun logFinish(strategy: String) {
        log.debug("Finished executing kotlin compiler using $strategy strategy")
    }

    @Synchronized
    override fun getDaemonConnection(environment: GradleCompilerEnvironment): DaemonConnection {
        val compilerId = CompilerId.makeCompilerId(environment.compilerClasspath)
        return newDaemonConnection(compilerId, flagFile, environment)
    }
}

