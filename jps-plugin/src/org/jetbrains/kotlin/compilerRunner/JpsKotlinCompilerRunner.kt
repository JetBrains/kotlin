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

import org.jetbrains.jps.api.GlobalOptions
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.mergeBeans
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.daemon.common.isDaemonEnabled
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class JpsKotlinCompilerRunner : KotlinCompilerRunner<JpsCompilerEnvironment>() {
    override val log: KotlinLogger = JpsKotlinLogger(KotlinBuilder.LOG)

    companion object {
        private @Volatile var jpsDaemonConnection: DaemonConnection? = null
    }

    fun runK2JvmCompiler(
            commonArguments: CommonCompilerArguments,
            k2jvmArguments: K2JVMCompilerArguments,
            compilerSettings: CompilerSettings,
            messageCollector: MessageCollector,
            environment: JpsCompilerEnvironment,
            moduleFile: File,
            collector: OutputItemsCollector
    ) {
        val arguments = mergeBeans(commonArguments, k2jvmArguments)
        setupK2JvmArguments(moduleFile, arguments)

        runCompiler(K2JVM_COMPILER, arguments, compilerSettings.additionalArguments, messageCollector, collector, environment)
    }

    fun runK2JsCompiler(
            commonArguments: CommonCompilerArguments,
            k2jsArguments: K2JSCompilerArguments,
            compilerSettings: CompilerSettings,
            messageCollector: MessageCollector,
            environment: JpsCompilerEnvironment,
            collector: OutputItemsCollector,
            sourceFiles: Collection<File>,
            libraryFiles: List<String>,
            outputFile: File
    ) {
        val arguments = mergeBeans(commonArguments, k2jsArguments)
        setupK2JsArguments(outputFile, sourceFiles, libraryFiles, arguments)

        runCompiler(K2JS_COMPILER, arguments, compilerSettings.additionalArguments, messageCollector, collector, environment)
    }

    override fun doRunCompiler(compilerClassName: String, argsArray: Array<String>, environment: JpsCompilerEnvironment, messageCollector: MessageCollector, collector: OutputItemsCollector): ExitCode {
        messageCollector.report(CompilerMessageSeverity.INFO, "Using kotlin-home = " + environment.kotlinPaths.homePath, CompilerMessageLocation.NO_LOCATION)

        return if (isDaemonEnabled()) {
            val daemonExitCode = compileWithDaemon(compilerClassName, argsArray, environment, messageCollector, collector)
            daemonExitCode ?: fallbackCompileStrategy(argsArray, collector, compilerClassName, environment, messageCollector)
        }
        else {
            fallbackCompileStrategy(argsArray, collector, compilerClassName, environment, messageCollector)
        }
    }

    private fun fallbackCompileStrategy(
            argsArray: Array<String>,
            collector: OutputItemsCollector,
            compilerClassName: String,
            environment: JpsCompilerEnvironment,
            messageCollector: MessageCollector
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

        val rc = CompilerRunnerUtil.invokeExecMethod(compilerClassName, argsArray, environment, messageCollector, out)

        // exec() returns an ExitCode object, class of which is loaded with a different class loader,
        // so we take it's contents through reflection
        val exitCode = ExitCode.valueOf(getReturnCodeFromObject(rc))
        processCompilerOutput(messageCollector, collector, stream, exitCode)
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

    private fun setupK2JsArguments( _outputFile: File, sourceFiles: Collection<File>, _libraryFiles: List<String>, settings: K2JSCompilerArguments) {
        with(settings) {
            noStdlib = true
            freeArgs = sourceFiles.map { it.path }
            outputFile = _outputFile.path
            metaInfo = true
            libraryFiles = _libraryFiles.toTypedArray()
        }
    }

    private fun getReturnCodeFromObject(rc: Any?): String {
        when {
            rc == null -> return INTERNAL_ERROR
            ExitCode::class.java.name == rc.javaClass.name -> return rc.toString()
            else -> throw IllegalStateException("Unexpected return: " + rc)
        }
    }

    @Synchronized
    override fun getDaemonConnection(environment: JpsCompilerEnvironment, messageCollector: MessageCollector): DaemonConnection {
        if (jpsDaemonConnection == null) {
            val libPath = CompilerRunnerUtil.getLibPath(environment.kotlinPaths, messageCollector)
            val compilerPath = File(libPath, "kotlin-compiler.jar")
            val flagFile = File.createTempFile("kotlin-compiler-jps-session-", "-is-running").apply {
                deleteOnExit()
            }
            newDaemonConnection(compilerPath, messageCollector, flagFile)
        }
        return jpsDaemonConnection!!
    }
}