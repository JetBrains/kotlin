/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl

import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.messages.DiagnosticMessageHolder
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.script.experimental.dependencies.ScriptDependencies

class ReplCompilerStageHistory(private val state: GenericReplCompilerState) : BasicReplStageHistory<ScriptDescriptor>(state.lock) {

    override fun reset(): Iterable<ILineId> {
        val removedCompiledLines = super.reset()
        val removedAnalyzedLines = state.analyzerEngine.reset()

        checkConsistent(removedCompiledLines, removedAnalyzedLines)
        return removedCompiledLines
    }

    override fun resetTo(id: ILineId): Iterable<ILineId> {
        val removedCompiledLines = super.resetTo(id)
        val removedAnalyzedLines = state.analyzerEngine.resetToLine(id)

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

abstract class GenericReplCheckerState : IReplStageState<ScriptDescriptor> {

    // "line" - is the unit of evaluation here, could in fact consists of several character lines
    class LineState(
        val codeLine: ReplCodeLine,
        val psiFile: KtFile,
        val errorHolder: DiagnosticMessageHolder
    )

    var lastLineState: LineState? = null // for transferring state to the compiler in most typical case
}

class GenericReplCompilerState(environment: KotlinCoreEnvironment, override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()) :
    IReplStageState<ScriptDescriptor>, GenericReplCheckerState() {

    override val history = ReplCompilerStageHistory(this)

    override val currentGeneration: Int get() = (history as BasicReplStageHistory<*>).currentGeneration.get()

    val analyzerEngine = ReplCodeAnalyzer(environment)

    var lastDependencies: ScriptDependencies? = null
}
