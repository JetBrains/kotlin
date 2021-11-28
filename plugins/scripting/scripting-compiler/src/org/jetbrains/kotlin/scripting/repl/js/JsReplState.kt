/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.cli.common.repl.BasicReplStageHistory
import org.jetbrains.kotlin.cli.common.repl.IReplStageState
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzerBase
import java.util.concurrent.locks.ReentrantReadWriteLock

class JsReplCompilationHistoryItem(
    val scriptSymbol: IrScriptSymbol
)

class JsReplCompilerStageHistory(lock: ReentrantReadWriteLock) : BasicReplStageHistory<JsReplCompilationHistoryItem>(lock)

// NOTE: the state management machinery is reduced in this implementation, since it is unused at the moment in the JS REPL (see JvmReplCompilerState for complete implementation, if needed)
class JsReplCompilerState(
    override val lock: ReentrantReadWriteLock,
    val nameTables: NameTables,
    val dependencies: List<ModuleDescriptor>,
    val analyzerState: ReplCodeAnalyzerBase.ResettableAnalyzerState,
    val symbolTable: SymbolTable
) : IReplStageState<JsReplCompilationHistoryItem> {

    override val history = JsReplCompilerStageHistory(lock)

    override val currentGeneration: Int get() = (history as BasicReplStageHistory<*>).currentGeneration.get()
}
