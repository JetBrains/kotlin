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

import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jps.api.GlobalOptions
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.additionalArgumentsAsList
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class JpsKotlinCompilerRunner {
    private val log: KotlinLogger = JpsKotlinLogger(KotlinBuilder.LOG)

    private var compilerSettings: CompilerSettings? = null

    private inline fun withCompilerSettings(settings: CompilerSettings, fn: () -> Unit) {
        val old = compilerSettings
        try {
            compilerSettings = settings
            fn()
        } finally {
            compilerSettings = old
        }
    }

    companion object {
        @Volatile
        private var _jpsCompileServiceSession: CompileServiceSession? = null

        @TestOnly
        fun shutdownDaemon() {
            _jpsCompileServiceSession?.let {
                try {
                    it.compileService.shutdown()
                } catch (_: Throwable) {
                }
            }
            _jpsCompileServiceSession = null
        }

        fun releaseCompileServiceSession() {
            _jpsCompileServiceSession?.let {
                try {
                    it.compileService.releaseCompileSession(it.sessionId)
                } catch (_: Throwable) {
                }
            }
            _jpsCompileServiceSession = null
        }

        @Synchronized
        private fun getOrCreateDaemonConnection(newConnection: () -> CompileServiceSession?): CompileServiceSession? {
            // TODO: consider adding state "ping" to the daemon interface
            if (_jpsCompileServiceSession == null || _jpsCompileServiceSession!!.compileService.getDaemonOptions() !is CompileService.CallResult.Good<DaemonOptions>) {
                releaseCompileServiceSession()
                _jpsCompileServiceSession = newConnection()
            }

            return _jpsCompileServiceSession
        }

        const val FAIL_ON_FALLBACK_PROPERTY = "test.kotlin.jps.compiler.runner.fail.on.fallback"
    }

    fun classesFqNamesByFiles(
        environment: JpsCompilerEnvironment,
        files: Set<File>
    ): Set<String> = withDaemonOrFallback(
        withDaemon = {
            doWithDaemon(environment) { sessionId, daemon ->
                daemon.classesFqNamesByFiles(
                    sessionId,
                    files.toSet() // convert to standard HashSet to avoid serialization issues
                )
            }
        },
        fallback = {
            CompilerRunnerUtil.invokeClassesFqNames(environment, files)
        }
    )

    fun runK2MetadataCompiler(
        commonArguments: CommonCompilerArguments,
        k2MetadataArguments: K2MetadataCompilerArguments,
        compilerSettings: CompilerSettings,
        environment: JpsCompilerEnvironment,
        destination: String,
        classpath: Collection<String>,
        sourceFiles: Collection<File>
    ) {
        val arguments = mergeBeans(commonArguments, XmlSerializerUtil.createCopy(k2MetadataArguments))

        val classpathSet = arguments.classpath?.split(File.pathSeparator)?.toMutableSet() ?: mutableSetOf()
        classpathSet.addAll(classpath)
        arguments.classpath = classpath.joinToString(File.pathSeparator)
        arguments.freeArgs = sourceFiles.map { it.absolutePath }
        arguments.destination = arguments.destination ?: destination

        withCompilerSettings(compilerSettings) {
            runCompiler(KotlinCompilerClass.METADATA, arguments, environment)
        }
    }

    fun runK2JvmCompiler(
        commonArguments: CommonCompilerArguments,
        k2jvmArguments: K2JVMCompilerArguments,
        compilerSettings: CompilerSettings,
        environment: JpsCompilerEnvironment,
        moduleFile: File
    ) {
        val arguments = mergeBeans(commonArguments, XmlSerializerUtil.createCopy(k2jvmArguments))
        setupK2JvmArguments(moduleFile, arguments)
        withCompilerSettings(compilerSettings) {
            runCompiler(KotlinCompilerClass.JVM, arguments, environment)
        }
    }

    fun runK2JsCompiler(
        commonArguments: CommonCompilerArguments,
        k2jsArguments: K2JSCompilerArguments,
        compilerSettings: CompilerSettings,
        environment: JpsCompilerEnvironment,
        allSourceFiles: Collection<File>,
        commonSources: Collection<File>,
        sourceMapRoots: Collection<File>,
        libraries: List<String>,
        friendModules: List<String>,
        outputFile: File
    ) {
        log.debug("K2JS: common arguments: " + ArgumentUtils.convertArgumentsToStringList(commonArguments))
        log.debug("K2JS: JS arguments: " + ArgumentUtils.convertArgumentsToStringList(k2jsArguments))

        val arguments = mergeBeans(commonArguments, XmlSerializerUtil.createCopy(k2jsArguments))
        log.debug("K2JS: merged arguments: " + ArgumentUtils.convertArgumentsToStringList(arguments))

        setupK2JsArguments(outputFile, allSourceFiles, commonSources, libraries, friendModules, arguments)
        if (arguments.sourceMap) {
            arguments.sourceMapBaseDirs = sourceMapRoots.joinToString(File.pathSeparator) { it.path }
        }

        log.debug("K2JS: arguments after setup" + ArgumentUtils.convertArgumentsToStringList(arguments))

        withCompilerSettings(compilerSettings) {
            runCompiler(KotlinCompilerClass.JS, arguments, environment)
        }
    }

    private fun compileWithDaemonOrFallback(
        compilerClassName: String,
        compilerArgs: CommonCompilerArguments,
        environment: JpsCompilerEnvironment
    ) {
        log.debug("Using kotlin-home = " + environment.kotlinPaths.homePath)

        withDaemonOrFallback(
            withDaemon = { compileWithDaemon(compilerClassName, compilerArgs, environment) },
            fallback = { fallbackCompileStrategy(compilerArgs, compilerClassName, environment) }
        )
    }

    private fun runCompiler(compilerClassName: String, compilerArgs: CommonCompilerArguments, environment: JpsCompilerEnvironment) {
        try {
            compileWithDaemonOrFallback(compilerClassName, compilerArgs, environment)
        } catch (e: Throwable) {
            MessageCollectorUtil.reportException(environment.messageCollector, e)
            reportInternalCompilerError(environment.messageCollector)
        }
    }

    private fun compileWithDaemon(
        compilerClassName: String,
        compilerArgs: CommonCompilerArguments,
        environment: JpsCompilerEnvironment
    ): Int? {
        val targetPlatform = when (compilerClassName) {
            KotlinCompilerClass.JVM -> CompileService.TargetPlatform.JVM
            KotlinCompilerClass.JS -> CompileService.TargetPlatform.JS
            KotlinCompilerClass.METADATA -> CompileService.TargetPlatform.METADATA
            else -> throw IllegalArgumentException("Unknown compiler type $compilerClassName")
        }
        val compilerMode = CompilerMode.JPS_COMPILER
        val verbose = compilerArgs.verbose
        val options = CompilationOptions(
            compilerMode,
            targetPlatform,
            reportCategories(verbose),
            reportSeverity(verbose),
            requestedCompilationResults = emptyArray()
        )
        return doWithDaemon(environment) { sessionId, daemon ->
            environment.withProgressReporter { progress ->
                progress.compilationStarted()
                daemon.compile(
                    sessionId,
                    withAdditionalCompilerArgs(compilerArgs),
                    options,
                    JpsCompilerServicesFacadeImpl(environment),
                    null
                )
            }
        }
    }

    private fun <T> withDaemonOrFallback(withDaemon: () -> T?, fallback: () -> T): T =
        if (isDaemonEnabled()) {
            withDaemon() ?: fallback()
        } else {
            fallback()
        }

    private fun <T> doWithDaemon(
        environment: JpsCompilerEnvironment,
        fn: (sessionId: Int, daemon: CompileService) -> CompileService.CallResult<T>
    ): T? {
        log.debug("Try to connect to daemon")
        val connection = getDaemonConnection(environment)
        if (connection == null) {
            log.info("Could not connect to daemon")
            return null
        }

        val (daemon, sessionId) = connection
        val res = fn(sessionId, daemon)
        // TODO: consider implementing connection retry, instead of fallback here
        return res.takeUnless { it is CompileService.CallResult.Dying }?.get()
    }

    private fun withAdditionalCompilerArgs(compilerArgs: CommonCompilerArguments): Array<String> {
        val allArgs = ArgumentUtils.convertArgumentsToStringList(compilerArgs) +
                (compilerSettings?.additionalArgumentsAsList ?: emptyList())
        return allArgs.toTypedArray()
    }

    private fun reportCategories(verbose: Boolean): Array<Int> {
        val categories =
            if (!verbose) {
                arrayOf(ReportCategory.COMPILER_MESSAGE, ReportCategory.EXCEPTION)
            } else {
                ReportCategory.values()
            }

        return categories.map { it.code }.toTypedArray()
    }


    private fun reportSeverity(verbose: Boolean): Int =
        if (!verbose) {
            ReportSeverity.INFO.code
        } else {
            ReportSeverity.DEBUG.code
        }

    private fun fallbackCompileStrategy(
        compilerArgs: CommonCompilerArguments,
        compilerClassName: String,
        environment: JpsCompilerEnvironment
    ) {
        if ("true" == System.getProperty("kotlin.jps.tests") && "true" == System.getProperty(FAIL_ON_FALLBACK_PROPERTY)) {
            error("Cannot compile with Daemon, see logs bellow. Fallback strategy is disabled in tests")
        }

        // otherwise fallback to in-process
        log.info("Compile in-process")

        val stream = ByteArrayOutputStream()
        val out = PrintStream(stream)

        // the property should be set at least for parallel builds to avoid parallel building problems (racing between destroying and using environment)
        // unfortunately it cannot be currently set by default globally, because it breaks many tests
        // since there is no reliable way so far to detect running under tests, switching it on only for parallel builds
        if (System.getProperty(GlobalOptions.COMPILE_PARALLEL_OPTION, "false").toBoolean())
            CompilerSystemProperties.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value = "true"

        val rc = environment.withProgressReporter { progress ->
            progress.compilationStarted()
            CompilerRunnerUtil.invokeExecMethod(compilerClassName, withAdditionalCompilerArgs(compilerArgs), environment, out)
        }

        // exec() returns an ExitCode object, class of which is loaded with a different class loader,
        // so we take it's contents through reflection
        val exitCode = ExitCode.valueOf(getReturnCodeFromObject(rc))
        processCompilerOutput(environment.messageCollector, environment.outputItemsCollector, stream, exitCode)
    }

    private fun setupK2JvmArguments(moduleFile: File, settings: K2JVMCompilerArguments) {
        with(settings) {
            buildFile = moduleFile.absolutePath
            destination = null
            noStdlib = true
            noReflect = true
            noJdk = true
        }
    }

    private fun setupK2JsArguments(
        _outputFile: File,
        allSourceFiles: Collection<File>,
        _commonSources: Collection<File>,
        _libraries: List<String>,
        _friendModules: List<String>,
        settings: K2JSCompilerArguments
    ) {
        with(settings) {
            noStdlib = true
            freeArgs = allSourceFiles.map { it.path }.toMutableList()
            commonSources = _commonSources.map { it.path }.toTypedArray()
            outputFile = _outputFile.path
            metaInfo = true
            libraries = _libraries.joinToString(File.pathSeparator)
            friendModules = _friendModules.joinToString(File.pathSeparator)
        }
    }

    private fun getReturnCodeFromObject(rc: Any?): String = when {
        rc == null -> ExitCode.INTERNAL_ERROR.toString()
        ExitCode::class.java.name == rc::class.java.name -> rc.toString()
        else -> throw IllegalStateException("Unexpected return: " + rc)
    }

    private fun getDaemonConnection(environment: JpsCompilerEnvironment): CompileServiceSession? =
        getOrCreateDaemonConnection {
            environment.progressReporter.progress("connecting to daemon")
            val libPath = CompilerRunnerUtil.getLibPath(environment.kotlinPaths, environment.messageCollector)
            val compilerPath = File(libPath, "kotlin-compiler.jar")
            val daemonJarPath = File(libPath, "kotlin-daemon.jar")
            val toolsJarPath = CompilerRunnerUtil.jdkToolsJar
            val compilerId = CompilerId.makeCompilerId(listOfNotNull(compilerPath, toolsJarPath, daemonJarPath))
            val daemonOptions = configureDaemonOptions()
            val additionalJvmParams = mutableListOf<String>()

            IncrementalCompilation.toJvmArgs(additionalJvmParams)

            val clientFlagFile = KotlinCompilerClient.getOrCreateClientFlagFile(daemonOptions)
            val sessionFlagFile = makeAutodeletingFlagFile("compiler-jps-session-", File(daemonOptions.runFilesPathOrDefault))

            environment.withProgressReporter { progress ->
                progress.progress("connecting to daemon")
                KotlinCompilerRunnerUtils.newDaemonConnection(
                    compilerId,
                    clientFlagFile,
                    sessionFlagFile,
                    environment.messageCollector,
                    log.isDebugEnabled,
                    daemonOptions,
                    additionalJvmParams.toTypedArray()
                )
            }
        }
}
