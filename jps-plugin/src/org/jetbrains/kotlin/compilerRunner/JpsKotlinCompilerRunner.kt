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
import org.jetbrains.jps.api.GlobalOptions
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.additionalArgumentsAsList
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class JpsKotlinCompilerRunner : KotlinCompilerRunner<JpsCompilerEnvironment>() {
    override val log: KotlinLogger = JpsKotlinLogger(KotlinBuilder.LOG)

    private var compilerSettings: CompilerSettings? = null

    private inline fun withCompilerSettings(settings: CompilerSettings, fn: ()->Unit) {
        val old = compilerSettings
        try {
            compilerSettings = settings
            fn()
        }
        finally {
            compilerSettings = old
        }
    }

    companion object {
        @Volatile
        private var _jpsCompileServiceSession: CompileServiceSession? = null

        @Synchronized
        private fun getOrCreateDaemonConnection(newConnection: ()-> CompileServiceSession?): CompileServiceSession? {
            if (_jpsCompileServiceSession == null) {
                _jpsCompileServiceSession = newConnection()
            }

            return _jpsCompileServiceSession
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
            runCompiler(K2JVM_COMPILER, arguments, environment)
        }
    }

    fun runK2JsCompiler(
            commonArguments: CommonCompilerArguments,
            k2jsArguments: K2JSCompilerArguments,
            compilerSettings: CompilerSettings,
            environment: JpsCompilerEnvironment,
            sourceFiles: Collection<File>,
            libraries: List<String>,
            friendModules: List<String>,
            outputFile: File
    ) {
        log.debug("K2JS: common arguments: " + ArgumentUtils.convertArgumentsToStringList(commonArguments))
        log.debug("K2JS: JS arguments: " + ArgumentUtils.convertArgumentsToStringList(k2jsArguments))

        val arguments = mergeBeans(commonArguments, XmlSerializerUtil.createCopy(k2jsArguments))
        log.debug("K2JS: merged arguments: " + ArgumentUtils.convertArgumentsToStringList(arguments))

        setupK2JsArguments(outputFile, sourceFiles, libraries, friendModules, arguments)
        log.debug("K2JS: arguments after setup" + ArgumentUtils.convertArgumentsToStringList(arguments))

        withCompilerSettings(compilerSettings) {
            runCompiler(K2JS_COMPILER, arguments, environment)
        }
    }

    override fun compileWithDaemonOrFallback(
            compilerClassName: String,
            compilerArgs: CommonCompilerArguments,
            environment: JpsCompilerEnvironment
    ): ExitCode {
        log.debug("Using kotlin-home = " + environment.kotlinPaths.homePath)

        return if (isDaemonEnabled()) {
            val daemonExitCode = compileWithDaemon(compilerClassName, compilerArgs, environment)
            daemonExitCode ?: fallbackCompileStrategy(compilerArgs, compilerClassName, environment)
        }
        else {
            fallbackCompileStrategy(compilerArgs, compilerClassName, environment)
        }
    }

    override fun compileWithDaemon(
            compilerClassName: String,
            compilerArgs: CommonCompilerArguments,
            environment: JpsCompilerEnvironment
    ): ExitCode? {
        val targetPlatform = when (compilerClassName) {
            K2JVM_COMPILER -> CompileService.TargetPlatform.JVM
            K2JS_COMPILER -> CompileService.TargetPlatform.JS
            K2METADATA_COMPILER -> CompileService.TargetPlatform.METADATA
            else -> throw IllegalArgumentException("Unknown compiler type $compilerClassName")
        }

        log.debug("Try to connect to daemon")
        val connection = getDaemonConnection(environment)
        if (connection == null) {
            log.info("Could not connect to daemon")
            return null
        }

        val (daemon, sessionId) = connection
        val compilerMode = CompilerMode.JPS_COMPILER
        val verbose = compilerArgs.verbose
        val options = CompilationOptions(compilerMode, targetPlatform, reportCategories(verbose), reportSeverity(verbose), requestedCompilationResults = emptyArray())
        val res = daemon.compile(sessionId, withAdditionalCompilerArgs(compilerArgs), options, JpsCompilerServicesFacadeImpl(environment), null)
        return exitCodeFromProcessExitCode(res.get())
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
            }
            else {
                ReportCategory.values()
            }

        return categories.map { it.code }.toTypedArray()
    }


    private fun reportSeverity(verbose: Boolean): Int =
            if (!verbose) {
                ReportSeverity.INFO.code
            }
            else {
                ReportSeverity.DEBUG.code
            }

    private fun fallbackCompileStrategy(
            compilerArgs: CommonCompilerArguments,
            compilerClassName: String,
            environment: JpsCompilerEnvironment
    ): ExitCode {
        // otherwise fallback to in-process
        log.info("Compile in-process")

        val stream = ByteArrayOutputStream()
        val out = PrintStream(stream)

        // the property should be set at least for parallel builds to avoid parallel building problems (racing between destroying and using environment)
        // unfortunately it cannot be currently set by default globally, because it breaks many tests
        // since there is no reliable way so far to detect running under tests, switching it on only for parallel builds
        if (System.getProperty(GlobalOptions.COMPILE_PARALLEL_OPTION, "false").toBoolean())
            System.setProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")

        val rc = CompilerRunnerUtil.invokeExecMethod(compilerClassName, withAdditionalCompilerArgs(compilerArgs), environment, out)

        // exec() returns an ExitCode object, class of which is loaded with a different class loader,
        // so we take it's contents through reflection
        val exitCode = ExitCode.valueOf(getReturnCodeFromObject(rc))
        processCompilerOutput(environment, stream, exitCode)
        return exitCode
    }

    private fun setupK2JvmArguments(moduleFile: File, settings: K2JVMCompilerArguments) {
        with(settings) {
            module = moduleFile.absolutePath
            noStdlib = true
            noReflect = true
            noJdk = true
        }
    }

    private fun setupK2JsArguments(_outputFile: File, sourceFiles: Collection<File>, _libraries: List<String>, _friendModules: List<String>, settings: K2JSCompilerArguments) {
        with(settings) {
            noStdlib = true
            freeArgs = sourceFiles.map { it.path }.toMutableList()
            outputFile = _outputFile.path
            metaInfo = true
            libraries = _libraries.joinToString(File.pathSeparator)
            friendModules = _friendModules.joinToString(File.pathSeparator)
        }
    }

    private fun getReturnCodeFromObject(rc: Any?): String {
        when {
            rc == null -> return INTERNAL_ERROR
            ExitCode::class.java.name == rc::class.java.name -> return rc.toString()
            else -> throw IllegalStateException("Unexpected return: " + rc)
        }
    }

    override fun getDaemonConnection(environment: JpsCompilerEnvironment): CompileServiceSession? =
            getOrCreateDaemonConnection {
                val libPath = CompilerRunnerUtil.getLibPath(environment.kotlinPaths, environment.messageCollector)
                val compilerPath = File(libPath, "kotlin-compiler.jar")
                val toolsJarPath = CompilerRunnerUtil.getJdkToolsJar()
                val compilerId = CompilerId.makeCompilerId(listOfNotNull(compilerPath, toolsJarPath) )
                val daemonOptions = configureDaemonOptions()

                val clientFlagFile = KotlinCompilerClient.getOrCreateClientFlagFile(daemonOptions)
                val sessionFlagFile = makeAutodeletingFlagFile("compiler-jps-session-", File(daemonOptions.runFilesPathOrDefault))
                newDaemonConnection(compilerId, clientFlagFile, sessionFlagFile, environment, daemonOptions)
            }
}
