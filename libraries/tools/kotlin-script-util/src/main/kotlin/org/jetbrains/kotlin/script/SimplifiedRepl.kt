/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.script

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.repl.GenericRepl
import org.jetbrains.kotlin.cli.jvm.repl.findRequiredScriptingJarFiles
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.utils.PathUtil
import java.io.Closeable
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KClass
import kotlin.script.templates.standard.ScriptTemplateWithArgs

private val EMPTY_SCRIPT_ARGS: Array<out Any?> = arrayOf(emptyArray<String>())
private val EMPTY_SCRIPT_ARGS_TYPES: Array<out KClass<out Any>> = arrayOf(Array<String>::class)

open class CustomizedGenericRepl(disposable: Disposable,
                                 scriptDefinition: KotlinScriptDefinition,
                                 compilerConfiguration: CompilerConfiguration,
                                 messageCollector: MessageCollector,
                                 baseClassloader: ClassLoader?,
                                 fallbackScriptArgs: ScriptArgsWithTypes? = null,
                                 repeatingMode: ReplRepeatingMode,
                                 stateLock: ReentrantReadWriteLock)
    : GenericRepl(disposable, scriptDefinition, compilerConfiguration, messageCollector, baseClassloader, fallbackScriptArgs, repeatingMode, stateLock)

open class SimplifiedRepl protected constructor(protected val disposable: Disposable,
                                                protected val scriptDefinition: KotlinScriptDefinition,
                                                protected val compilerConfiguration: CompilerConfiguration,
                                                protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE,
                                                protected val sharedHostClassLoader: ClassLoader? = null,
                                                protected val emptyArgsProvider: ScriptTemplateEmptyArgsProvider,
                                                protected val stateLock: ReentrantReadWriteLock = ReentrantReadWriteLock()) : Closeable {

    constructor(disposable: Disposable = Disposer.newDisposable(),
                moduleName: String = "kotlin-script-module-${System.currentTimeMillis()}",
                additionalClasspath: List<File> = emptyList(),
                scriptDefinition: KotlinScriptDefinitionEx = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class, ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES)),
                messageCollector: MessageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false),
                repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE,
                sharedHostClassLoader: ClassLoader? = null) : this(disposable,
                                                                   compilerConfiguration = CompilerConfiguration().apply {
                                                                       addJvmClasspathRoots(PathUtil.getJdkClassesRoots())
                                                                       addJvmClasspathRoots(findRequiredScriptingJarFiles(scriptDefinition.template,
                                                                                                                          includeScriptEngine = false,
                                                                                                                          includeKotlinCompiler = false,
                                                                                                                          includeStdLib = true,
                                                                                                                          includeRuntime = true))
                                                                       addJvmClasspathRoots(additionalClasspath)
                                                                       put(CommonConfigurationKeys.MODULE_NAME, moduleName)
                                                                       put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
                                                                   },
                                                                   repeatingMode = repeatingMode,
                                                                   sharedHostClassLoader = sharedHostClassLoader,
                                                                   scriptDefinition = scriptDefinition,
                                                                   emptyArgsProvider = scriptDefinition)

    private val baseClassloader = URLClassLoader(compilerConfiguration.jvmClasspathRoots.map { it.toURI().toURL() }
                                                         .toTypedArray(), sharedHostClassLoader)

    var fallbackArgs: ScriptArgsWithTypes? = emptyArgsProvider.defaultEmptyArgs
        get() = stateLock.read { field }
        set(value) = stateLock.write { field = value }

    private val engine: GenericRepl by lazy {
        CustomizedGenericRepl(disposable = disposable,
                              scriptDefinition = scriptDefinition,
                              compilerConfiguration = compilerConfiguration,
                              messageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false),
                              baseClassloader = baseClassloader,
                              fallbackScriptArgs = fallbackArgs,
                              repeatingMode = repeatingMode,
                              stateLock = stateLock)
    }


    fun nextCodeLine(code: String) = engine.nextCodeLine(code)

    fun resetToLine(lineNumber: Int): List<ReplCodeLine> = engine.resetToLine(lineNumber)

    fun resetToLine(line: ReplCodeLine): List<ReplCodeLine> = resetToLine(line.no)

    val compiledHistory: List<ReplCodeLine> get() = engine.compiledHistory
    val evaluatedHistory: List<ReplCodeLine> get() = engine.evaluatedHistory

    val currentEvalClassPath: List<File> get() = stateLock.read { engine.currentClasspath }

    fun check(codeLine: ReplCodeLine): CheckResult {
        val result = engine.check(codeLine)
        return when (result) {
            is ReplCheckResponse.Error -> throw ReplCompilerException(result)
            is ReplCheckResponse.Ok -> CheckResult(codeLine, true)
            is ReplCheckResponse.Incomplete -> CheckResult(codeLine, false)
            else -> throw IllegalStateException("Unknown check result type ${result}")
        }
    }


    fun compileAndEval(codeLine: ReplCodeLine,
                       overrideScriptArgs: ScriptArgsWithTypes? = null,
                       verifyHistory: List<ReplCodeLine>? = null,
                       invokeWrapper: InvokeWrapper? = null): EvalResult {
        return engine.compileAndEval(codeLine, overrideScriptArgs ?: fallbackArgs, verifyHistory, invokeWrapper).toResult(codeLine)
    }

    fun compileAndEval(code: String,
                       overrideScriptArgs: ScriptArgsWithTypes? = null,
                       verifyHistory: List<ReplCodeLine>? = null,
                       invokeWrapper: InvokeWrapper? = null): EvalResult {
        return compileAndEval(nextCodeLine(code), overrideScriptArgs ?: fallbackArgs, verifyHistory, invokeWrapper)
    }

    @Deprecated("Unsafe to use individual compile/eval methods which may leave history state inconsistent across threads.", ReplaceWith("compileAndEval(codeLine)"))
    fun compile(codeLine: ReplCodeLine, verifyHistory: List<ReplCodeLine>? = null): CompileResult {
        return engine.compile(codeLine, verifyHistory).toResult()
    }

    @Deprecated("Unsafe to use individual compile/eval methods which may leave history state inconsistent across threads.", ReplaceWith("compileAndEval(codeLine)"))
    fun eval(compileResult: CompileResult,
             overrideScriptArgs: ScriptArgsWithTypes? = null,
             invokeWrapper: InvokeWrapper? = null): EvalResult {
        return engine.eval(compileResult.compilerData, overrideScriptArgs ?: fallbackArgs, invokeWrapper).toResult(compileResult.codeLine)
    }

    fun compileToEvaluable(codeLine: ReplCodeLine, verifyHistory: List<ReplCodeLine>? = null): Evaluable {
        val compiled = engine.compile(codeLine, verifyHistory)
        return if (compiled is ReplCompileResponse.CompiledClasses) {
            Evaluable(compiled, engine, fallbackArgs)
        }
        else {
            compiled.toResult()
            throw IllegalStateException("Unknown compiler result type ${this}")
        }
    }

    val lastEvaluatedScripts: List<EvalHistoryType> get() = engine.lastEvaluatedScripts

    override fun close() {
        disposable.dispose()
    }
}

class Evaluable(val compiledCode: ReplCompileResponse.CompiledClasses,
                private val evaluator: ReplEvaluator,
                private val fallbackArgs: ScriptArgsWithTypes? = null) {
    fun eval(scriptArgs: ScriptArgsWithTypes? = null, invokeWrapper: InvokeWrapper? = null): EvalResult {
        return evaluator.eval(compiledCode, scriptArgs ?: fallbackArgs, invokeWrapper).toResult(compiledCode.compiledCodeLine.source)
    }
}

private fun ReplCompileResponse.toResult(): CompileResult {
    return when (this) {
        is ReplCompileResponse.Error -> throw ReplCompilerException(this)
        is ReplCompileResponse.HistoryMismatch -> throw ReplCompilerException(this)
        is ReplCompileResponse.Incomplete -> throw ReplCompilerException(this)
        is ReplCompileResponse.CompiledClasses -> {
            CompileResult(this.compiledCodeLine.source, this)
        }
        else -> throw IllegalStateException("Unknown compiler result type ${this}")
    }
}

private fun ReplEvalResponse.toResult(codeLine: ReplCodeLine): EvalResult {
    return when (this) {
        is ReplEvalResponse.Error.CompileTime -> throw ReplCompilerException(this)
        is ReplEvalResponse.Error.Runtime -> throw ReplEvalRuntimeException(this)
        is ReplEvalResponse.HistoryMismatch -> throw ReplCompilerException(this)
        is ReplEvalResponse.Incomplete -> throw ReplCompilerException(this)
        is ReplEvalResponse.UnitResult -> {
            EvalResult(codeLine, Unit, this.completedEvalHistory)
        }
        is ReplEvalResponse.ValueResult -> {
            EvalResult(codeLine, this.value, this.completedEvalHistory)
        }
        else -> throw IllegalStateException("Unknown eval result type ${this}")
    }
}

class ReplCompilerException(val errorResult: ReplCompileResponse.Error) : Exception(errorResult.message) {
    constructor (checkResult: ReplCheckResponse.Error) : this(ReplCompileResponse.Error(emptyList(), checkResult.message, checkResult.location))
    constructor (incompleteResult: ReplCompileResponse.Incomplete) : this(ReplCompileResponse.Error(incompleteResult.compiledHistory, "Incomplete Code", CompilerMessageLocation.NO_LOCATION))
    constructor (historyMismatchResult: ReplCompileResponse.HistoryMismatch) : this(ReplCompileResponse.Error(historyMismatchResult.compiledHistory, "History Mismatch", CompilerMessageLocation.create(null, historyMismatchResult.lineNo, 0, null)))
    constructor (checkResult: ReplEvalResponse.Error.CompileTime) : this(ReplCompileResponse.Error(checkResult.completedEvalHistory, checkResult.message, checkResult.location))
    constructor (incompleteResult: ReplEvalResponse.Incomplete) : this(ReplCompileResponse.Error(incompleteResult.completedEvalHistory, "Incomplete Code", CompilerMessageLocation.NO_LOCATION))
    constructor (historyMismatchResult: ReplEvalResponse.HistoryMismatch) : this(ReplCompileResponse.Error(historyMismatchResult.completedEvalHistory, "History Mismatch", CompilerMessageLocation.create(null, historyMismatchResult.lineNo, 0, null)))
}

class ReplEvalRuntimeException(val errorResult: ReplEvalResponse.Error.Runtime) : Exception(errorResult.message, errorResult.cause)

data class CheckResult(val codeLine: ReplCodeLine, val isComplete: Boolean = true)
data class CompileResult(val codeLine: ReplCodeLine,
                         val compilerData: ReplCompileResponse.CompiledClasses)

data class EvalResult(val codeLine: ReplCodeLine, val resultValue: Any?, val evalHistory: List<ReplCodeLine>)
