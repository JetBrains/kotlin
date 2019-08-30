/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js.test

import org.jetbrains.kotlin.cli.common.repl.ReplCompiler
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.scripting.repl.js.*

class JsReplTestAgainstBinaries : AbstractJsReplTest() {
    private val dependencyLoader = DependencyLoader()

    private val runtimeBinary: String
    private val nameTable: NameTables

    init {
        val dependencies = readLibrariesFromConfiguration(environment.configuration)
        val compiler = ScriptDependencyCompiler(environment)

        val result = compiler.compile(dependencies)
        runtimeBinary = result.first
        nameTable = result.second

        dependencyLoader.saveScriptDependencyBinary(runtimeBinary)
        dependencyLoader.saveNames(nameTable)
    }

    override fun createCompiler(): ReplCompiler {
        return JsDebuggerCompiler(environment, dependencyLoader.loadNames())
    }

    override fun preprocessEvaluation() {
        jsEvaluator.eval(
            jsEvaluator.createState(),
            createCompileResult(dependencyLoader.loadScriptDependencyBinary())
        )
    }

    override fun close() {
        //do nothing
    }
}
