/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ast.directives

import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.js.testOld.utils.AstSearchUtil
import java.io.File

class CheckDeclarationExistsDirective(
    entry: String,
    private val declarationKind: DeclarationKind
) : ArgumentsHelper(entry), JsAstDirective {
    enum class DeclarationKind {
        CLASS,
        FUNCTION
        ;
    }

    val declarationName by positional(0)

    override fun evaluate(ast: JsNode, sourceFile: File) {
        when (declarationKind) {
            DeclarationKind.CLASS -> AstSearchUtil.getClass(ast, declarationName)
            DeclarationKind.FUNCTION -> AstSearchUtil.getFunction(ast, declarationName)
        }
    }
}
