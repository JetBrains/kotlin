/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ast.directives

import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.sideEffects
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.js.testOld.utils.AstSearchUtil.getClass
import org.jetbrains.kotlin.js.testOld.utils.AstSearchUtil.getFunction
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import java.io.File

class FunctionHasEffectsDirective(entry: String) : ArgumentsHelper(entry), JsAstDirective {
    override fun evaluate(ast: JsNode, sourceFile: File) {
        val functionType = getPositionalArgument(0)
        val functionName = getPositionalArgument(1)
        val effectName = getPositionalArgument(2)
        val [function, description] = when (functionType) {
            "function" -> getFunction(ast, functionName) to "Function"
            "class" -> getClass(ast, functionName).constructor to "Constructor"
            else -> throw IllegalArgumentException("Function type has to be 'class' or 'function' (got '$functionType')")
        }
        checkNotNull(function) { "No constructor in class" }
        val actual = function.sideEffects
        val expected = when (effectName) {
            "PURE" -> SideEffectKind.PURE
            "READ" -> SideEffectKind.DEPENDS_ON_STATE
            "WRITE" -> SideEffectKind.AFFECTS_STATE
            else -> throw IllegalArgumentException("Invalid side effect name: '$effectName'")
        }
        assertEquals(expected, actual) { "$description $functionName" }
    }
}
