/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.rmi.*
import org.jetbrains.kotlin.rmi.kotlinr.*
import java.io.*
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.TimeUnit

public object KotlinCompilerRunner {
    private val K2JVM_COMPILER = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
    private val K2JS_COMPILER = "org.jetbrains.kotlin.cli.js.K2JSCompiler"
    private val INTERNAL_ERROR = ExitCode.INTERNAL_ERROR.toString()

    public fun runK2JvmCompiler(
            commonArguments: CommonCompilerArguments,
            k2jvmArguments: K2JVMCompilerArguments,
            compilerSettings: CompilerSettings,
            messageCollector: MessageCollector,
            environment: CompilerEnvironment,
            moduleFile: File,
            collector: OutputItemsCollector) {
        val arguments = mergeBeans(commonArguments, k2jvmArguments)
        setupK2JvmArguments(moduleFile, arguments)

        runCompiler(K2JVM_COMPILER, arguments, compilerSettings.additionalArguments, messageCollector, collector, environment)
    }

    public fun runK2JsCompiler(
            commonArguments: CommonCompilerArguments,
            k2jsArguments: K2JSCompilerArguments,
            compilerSettings: CompilerSettings,
            messageCollector: MessageCollector,
            environment: CompilerEnvironment,
            collector: OutputItemsCollector,
            sourceFiles: Collection<File>,
            libraryFiles: List<String>,
            outputFile: File) {
        val arguments = mergeBeans(commonArguments, k2jsArguments)
        setupK2JsArguments(outputFile, sourceFiles, libraryFiles, arguments)

        runCompiler(K2JS_COMPILER, arguments, compilerSettings.additionalArguments, messageCollector, collector, environment)
    }

    private fun processCompilerOutput(
            messageCollector: MessageCollector,
            collector: OutputItemsCollector,
            stream: ByteArrayOutputStream,
            exitCode: String) {
        val reader = BufferedReader(StringReader(stream.toString()))
        CompilerOutputParser.parseCompilerMessagesFromReader(messageCollector, reader, collector)

        if (INTERNAL_ERROR == exitCode) {
            reportInternalCompilerError(messageCollector)
        }
    }

    private fun reportInternalCompilerError(messageCollector: MessageCollector) {
        messageCollector.report(ERROR, "Compiler terminated with internal error", CompilerMessageLocation.NO_LOCATION)
    }

    private fun runCompiler(
            compilerClassName: String,
            arguments: CommonCompilerArguments,
            additionalArguments: String,
            messageCollector: MessageCollector,
            collector: OutputItemsCollector,
            environment: CompilerEnvironment) {
        try {
            messageCollector.report(INFO, "Using kotlin-home = " + environment.kotlinPaths.homePath, CompilerMessageLocation.NO_LOCATION)

            val argumentsList = ArgumentUtils.convertArgumentsToStringList(arguments)
            argumentsList.addAll(additionalArguments.split(" "))

            val argsArray = argumentsList.toTypedArray()

            if (!tryCompileWithDaemon(compilerClassName, argsArray, environment, messageCollector, collector)) {
                // otherwise fallback to in-process

                val stream = ByteArrayOutputStream()
                val out = PrintStream(stream)

                val rc = CompilerRunnerUtil.invokeExecMethod(compilerClassName, argsArray, environment, messageCollector, out)

                // exec() returns an ExitCode object, class of which is loaded with a different class loader,
                // so we take it's contents through reflection
                processCompilerOutput(messageCollector, collector, stream, getReturnCodeFromObject(rc))
            }
        }
        catch (e: Throwable) {
            MessageCollectorUtil.reportException(messageCollector, e)
            reportInternalCompilerError(messageCollector)
        }

    }

    internal class DaemonConnection(public val daemon: CompileService?)

    internal object getDaemonConnection {
        private var connection: DaemonConnection? = null

        operator fun invoke(environment: CompilerEnvironment, messageCollector: MessageCollector): DaemonConnection? {
            if (connection == null) {
                val libPath = CompilerRunnerUtil.getLibPath(environment.kotlinPaths, messageCollector)
                val compilerId = CompilerId.makeCompilerId(File(libPath, "kotlin-compiler.jar"))
                val daemonOptions = configureDaemonOptions()
                val daemonJVMOptions = configureDaemonJVMOptions(true)

                val daemonReportMessages = ArrayList<DaemonReportMessage>()

                val profiler = if (daemonOptions.reportPerf) WallAndThreadAndMemoryTotalProfiler(withGC = false) else DummyProfiler()

                profiler.withMeasure(null) {
                    connection = DaemonConnection(
                            KotlinCompilerClient.connectToCompileService(compilerId, daemonJVMOptions, daemonOptions, DaemonReportingTargets(null, daemonReportMessages), true, true)
                    )
                }

                for (msg in daemonReportMessages) {
                    messageCollector.report(CompilerMessageSeverity.INFO,
                                            (if (msg.category == DaemonReportCategory.EXCEPTION && connection?.daemon == null)  "Falling  back to compilation without daemon due to error: " else "") + msg.message,
                                            CompilerMessageLocation.NO_LOCATION)
                }

                reportTotalAndThreadPerf("Daemon connect", daemonOptions, messageCollector, profiler)
            }
            return connection
        }
    }

    private fun tryCompileWithDaemon(compilerClassName: String,
                                     argsArray: Array<String>,
                                     environment: CompilerEnvironment,
                                     messageCollector: MessageCollector,
                                     collector: OutputItemsCollector): Boolean {

        if (isDaemonEnabled()) {

            val connection = getDaemonConnection(environment, messageCollector)

            if (connection?.daemon != null) {
                val compilerOut = ByteArrayOutputStream()
                val daemonOut = ByteArrayOutputStream()

                val services = CompilationServices(
                        incrementalCompilationComponents = environment.services.get(IncrementalCompilationComponents::class.java),
                        compilationCanceledStatus = environment.services.get(CompilationCanceledStatus::class.java))

                val targetPlatform = when (compilerClassName) {
                    K2JVM_COMPILER -> CompileService.TargetPlatform.JVM
                    K2JS_COMPILER -> CompileService.TargetPlatform.JS
                    else -> throw IllegalArgumentException("Unknown compiler type $compilerClassName")
                }
                val res = KotlinCompilerClient.incrementalCompile(connection!!.daemon!!, targetPlatform, argsArray, services, compilerOut, daemonOut)

                processCompilerOutput(messageCollector, collector, compilerOut, res.toString())
                BufferedReader(StringReader(daemonOut.toString())).forEachLine {
                    messageCollector.report(CompilerMessageSeverity.INFO, it, CompilerMessageLocation.NO_LOCATION)
                }
                return true
            }
        }
        return false
    }

    private fun reportTotalAndThreadPerf(message: String, daemonOptions: DaemonOptions, messageCollector: MessageCollector, profiler: Profiler) {
        if (daemonOptions.reportPerf) {
            fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)
            val counters = profiler.getTotalCounters()
            messageCollector.report(INFO,
                                    "PERF: $message ${counters.time.ms()} ms, thread ${counters.threadTime.ms()}",
                                    CompilerMessageLocation.NO_LOCATION)
        }
    }

    private fun getReturnCodeFromObject(rc: Any?): String {
        when {
            rc == null -> return INTERNAL_ERROR
            ExitCode::class.java.name == rc.javaClass.name -> return rc.toString()
            else -> throw IllegalStateException("Unexpected return: " + rc)
        }
    }

    private fun <T : CommonCompilerArguments> mergeBeans(from: CommonCompilerArguments, to: T): T {
        // TODO: rewrite when updated version of com.intellij.util.xmlb is available on TeamCity
        val copy = XmlSerializerUtil.createCopy(to)

        val fromFields = collectFieldsToCopy(from.javaClass)
        for (fromField in fromFields) {
            val toField = copy.javaClass.getField(fromField.name)
            toField.set(copy, fromField.get(from))
        }

        return copy
    }

    private fun collectFieldsToCopy(clazz: Class<*>): List<Field> {
        val fromFields = ArrayList<Field>()

        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            for (field in currentClass.declaredFields) {
                val modifiers = field.modifiers
                if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                    fromFields.add(field)
                }
            }
            currentClass = currentClass.superclass
        }

        return fromFields
    }

    private fun setupK2JvmArguments(moduleFile: File, settings: K2JVMCompilerArguments) {
        with(settings) {
            module = moduleFile.absolutePath
            noStdlib = true
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
}
