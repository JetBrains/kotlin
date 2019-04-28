/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.scripting.repl.ReplCodeAnalyzer
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvmhost.impl.KJvmCompilerImpl
import kotlin.script.experimental.jvmhost.impl.withDefaults

class JvmReplCompiler(
    val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    val hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    val replCompilerProxy: KJvmReplCompilerProxy = KJvmCompilerImpl(hostConfiguration.withDefaults())
) : ReplCompiler {

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = JvmReplCompilerState(replCompilerProxy, lock)

    override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}

interface KJvmReplCompilerProxy {
    fun createReplCompilationState(scriptCompilationConfiguration: ScriptCompilationConfiguration): JvmReplCompilerState.Compilation
}

class JvmReplCompilerStageHistory(private val state: JvmReplCompilerState) :
    BasicReplStageHistory<ScriptDescriptor>(state.lock) {

    override fun reset(): Iterable<ILineId> {
        val removedCompiledLines = super.reset()
        val removedAnalyzedLines = state.compilation.analyzerEngine.reset()

        checkConsistent(removedCompiledLines, removedAnalyzedLines)
        return removedCompiledLines
    }

    override fun resetTo(id: ILineId): Iterable<ILineId> {
        val removedCompiledLines = super.resetTo(id)
        val removedAnalyzedLines = state.compilation.analyzerEngine.resetToLine(id)

        checkConsistent(removedCompiledLines, removedAnalyzedLines)
        return removedCompiledLines
    }

    private fun checkConsistent(removedCompiledLines: Iterable<ILineId>, removedAnalyzedLines: List<ReplCodeLine>) {
        removedCompiledLines.zip(removedAnalyzedLines).forEach { (removedCompiledLine, removedAnalyzedLine) ->
            if (removedCompiledLine != LineId(removedAnalyzedLine)) {
                throw IllegalStateException("History mismatch when resetting lines: ${removedCompiledLine.no} != $removedAnalyzedLine")
            }
        }
    }
}

class JvmReplCompilerState(
    val replCompilerProxy: KJvmReplCompilerProxy,
    override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
) : IReplStageState<ScriptDescriptor> {

    override val history = JvmReplCompilerStageHistory(this)

    override val currentGeneration: Int get() = (history as BasicReplStageHistory<*>).currentGeneration.get()

    override fun dispose() {
        lock.write {
            _compilation?.disposable?.dispose()
            _compilation = null
            super.dispose()
        }
    }

    fun getCompilationState(scriptCompilationConfiguration: ScriptCompilationConfiguration): Compilation = lock.write {
        if (_compilation == null) {
            initializeCompilation(scriptCompilationConfiguration)
        }
        _compilation!!
    }

    internal val compilation: Compilation
        get() = _compilation ?: throw IllegalStateException("Compilation state is either not initializad or already destroyed")

    private var _compilation: Compilation? = null

    val isCompilationInitialized get() = _compilation != null

    private fun initializeCompilation(scriptCompilationConfiguration: ScriptCompilationConfiguration) {
        if (_compilation != null) throw IllegalStateException("Compilation state is already initialized")
        _compilation = replCompilerProxy.createReplCompilationState(scriptCompilationConfiguration)
    }

    interface Compilation {
        val disposable: Disposable
        val baseScriptCompilationConfiguration: ScriptCompilationConfiguration
        val environment: KotlinCoreEnvironment
        val analyzerEngine: ReplCodeAnalyzer
    }
}
