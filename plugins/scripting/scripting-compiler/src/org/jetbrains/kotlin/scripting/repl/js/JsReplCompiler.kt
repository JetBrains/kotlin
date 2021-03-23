/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzerBase
import java.util.concurrent.locks.ReentrantReadWriteLock

// Used to compile REPL code lines
class JsReplCompiler(private val environment: KotlinCoreEnvironment) : ReplCompiler {

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> {
        return JsReplCompilerState(
            lock,
            NameTables(emptyList(), mappedNames = mutableMapOf()),
            readLibrariesFromConfiguration(environment.configuration),
            ReplCodeAnalyzerBase.ResettableAnalyzerState(),
            SymbolTable(IdSignatureDescriptor(JsManglerDesc /* TODO */), IrFactoryImpl)
        )
    }

    override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult {
        return ReplCheckResult.Ok()
    }

    override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult {
        val compilationState = state.asState(JsReplCompilerState::class.java)
        return JsCoreScriptingCompiler(
            environment,
            compilationState.nameTables,
            compilationState.symbolTable,
            compilationState.dependencies,
            compilationState
        ).compile(codeLine)
    }
}
