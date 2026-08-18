/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ast.directives

import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.js.testOld.utils.AstSearchUtil.getClass
import org.jetbrains.kotlin.js.testOld.utils.AstSearchUtil.getFunction
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import java.io.File

class ExpectGeneratedJsDirective(entry: String) : ArgumentsHelper(entry), JsAstDirective {
    val functionNames by list("function")
    val classNames by list("class")
    val expect by required()

    override fun evaluate(ast: JsNode, sourceFile: File) {
        val expectedFile = File(sourceFile.getParentFile(), expect)
        val code = functionNames.joinToString(separator = "\n", postfix = "\n") { getFunction(ast, it).toString() } +
                classNames.joinToString(separator = "\n", postfix = "\n") { getClass(ast, it).toString() }
        assertEqualsToFile(expectedFile, code, message = {
            "Functions ${functionNames.joinToString()} or classes ${classNames.joinToString()} got different generated JS code"
        })
    }
}
