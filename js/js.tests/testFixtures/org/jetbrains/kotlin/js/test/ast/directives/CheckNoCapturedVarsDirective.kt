/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ast.directives

import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.inline.util.collectFreeVariables
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.js.testOld.utils.AstSearchUtil
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.io.File

class CheckNoCapturedVarsDirective(entry: String) : ArgumentsHelper(entry), JsAstDirective {
    val function by required()
    val except by set()

    override fun evaluate(ast: JsNode, sourceFile: File) {
        val function = AstSearchUtil.getFunction(ast, function)
        val freeVars = function.collectFreeVariables()
        for (freeVar in freeVars) {
            assertTrue(freeVar.ident in except) { "Function ${this.function} captures free variable ${freeVar.ident}" }
        }
    }
}
