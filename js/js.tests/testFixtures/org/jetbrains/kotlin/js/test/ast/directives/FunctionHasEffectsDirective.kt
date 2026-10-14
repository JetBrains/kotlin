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
import org.jetbrains.kotlin.utils.addToStdlib.unreachableBranch
import java.io.File

class FunctionHasEffectsDirective(entry: String) : ArgumentsHelper(entry), JsAstDirective {
    val function by optional()
    val constructor by optional()

    override fun evaluate(ast: JsNode, sourceFile: File) {
        val functionName = this.function
        val className = this.constructor
        val effectName = getPositionalArgument(0)

        if ((functionName == null) == (className == null)) {
            throw IllegalArgumentException("Expected exactly one of `function` or `constructor`.")
        }

        val [function, description] = when {
            functionName != null -> getFunction(ast, functionName) to "Function $functionName"
            className != null -> getClass(ast, className).constructor to "Constructor of $className"
            else -> unreachableBranch("checked in the if above")
        }

        checkNotNull(function) { "$description not found" }

        val actual = function.sideEffects
        val expected = when (effectName) {
            "PURE" -> SideEffectKind.PURE
            "READ" -> SideEffectKind.DEPENDS_ON_STATE
            "WRITE" -> SideEffectKind.AFFECTS_STATE
            else -> throw IllegalArgumentException("Invalid side effect name: '$effectName'")
        }
        assertEquals(expected, actual) { description }
    }
}
