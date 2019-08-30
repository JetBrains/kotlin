/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js.test

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.repl.ReplCompiler
import org.jetbrains.kotlin.scripting.repl.js.*

class JsReplTestAgainstKlib : AbstractJsReplTest() {
    override fun createCompiler(): ReplCompiler = JsReplCompiler(environment)

    override fun preprocessEvaluation() {
        val scriptDependencyBinary = (compiler as JsReplCompiler).scriptDependencyBinary

        jsEvaluator.eval(
            jsEvaluator.createState(),
            createCompileResult(scriptDependencyBinary)
        )
    }

    override fun close() {
        Disposer.dispose(disposable)
    }
}
