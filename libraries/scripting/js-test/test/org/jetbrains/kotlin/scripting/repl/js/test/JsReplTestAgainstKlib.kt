/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js.test

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.engine.ScriptEngineNashorn
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzer
import org.jetbrains.kotlin.scripting.repl.js.*
import java.util.concurrent.locks.ReentrantReadWriteLock

class JsReplTestAgainstKlib : AbstractJsReplTest() {

    private var dependencyCode: String? = null

    override fun createCompilationState(): JsReplCompilationState {
        val nameTables = NameTables(emptyList())
        val mangler = JsManglerDesc
        val signaturer = IdSignatureDescriptor(mangler)
        val symbolTable = SymbolTable(signaturer)
        val dependencyCompiler = JsScriptDependencyCompiler(environment.configuration, nameTables, symbolTable)
        val dependencies = readLibrariesFromConfiguration(environment.configuration)
        dependencyCode = dependencyCompiler.compile(dependencies)

        return JsReplCompilationState(
            ReentrantReadWriteLock(),
            nameTables,
            dependencies,
            ReplCodeAnalyzer.ResettableAnalyzerState(),
            symbolTable
        )
    }

    override fun createEvaluationState(): JsEvaluationState {
        val state = JsEvaluationState(ReentrantReadWriteLock(), ScriptEngineNashorn())

        JsReplEvaluator().eval(state, createCompileResult(dependencyCode ?: error("Dependencies has to be compiled first")))

        dependencyCode = null

        return state
    }

    override fun close() {
        Disposer.dispose(disposable)
    }
}
