/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js.test

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.engine.ScriptEngineNashorn
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzerBase
import org.jetbrains.kotlin.scripting.repl.js.*
import java.util.concurrent.locks.ReentrantReadWriteLock

// 1. Compile dependencies
// 2. Save them as a binary dependency (name table and js string)
// 3. For each new state load dependency's table and js code
class JsReplTestAgainstBinaries : AbstractJsReplTest() {
    private val dependencyLoader = DependencyLoader()
    private val dependencies = readLibrariesFromConfiguration(environment.configuration)

    init {
        val nameTable = NameTables(emptyList(), mappedNames = mutableMapOf())
        val compiler = JsScriptDependencyCompiler(environment.configuration, nameTable, createSymbolTable())
        val runtimeBinary = compiler.compile(dependencies)

        dependencyLoader.saveScriptDependencyBinary(runtimeBinary)
        dependencyLoader.saveNames(nameTable)
    }

    override fun createCompilationState(): JsReplCompilerState {
        val replState = ReplCodeAnalyzerBase.ResettableAnalyzerState()
        return JsReplCompilerState(ReentrantReadWriteLock(), dependencyLoader.loadNames(), dependencies, replState, createSymbolTable())
    }

    private fun createSymbolTable(): SymbolTable =
        SymbolTable(IdSignatureDescriptor(JsManglerDesc), IrFactoryImpl)

    override fun createEvaluationState(): JsEvaluationState {
        val state = JsEvaluationState(ReentrantReadWriteLock(), ScriptEngineNashorn())
        JsReplEvaluator().eval(state, createCompileResult(dependencyLoader.loadScriptDependencyBinary()))
        return state
    }

    override fun close() {
        //do nothing
    }
}
