/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ast.directives

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.io.File

class CheckCommentExistsDirective(entry: String) : ArgumentsHelper(entry), JsAstDirective {
    val text by required()
    val multiline by boolean()

    private fun isNeededCommentType(comment: JsComment): Boolean =
        if (multiline) comment is JsMultiLineComment else comment is JsSingleLineComment

    private fun isTheSameText(str1: String, str2: String): Boolean {
        val lines1 = str1.lines()
        val lines2 = str2.lines()

        if (lines1.size != lines2.size) return false

        for (i in lines1.indices) {
            if (lines1[i].trim() != lines2[i].trim()) return false
        }

        return true
    }

    override fun evaluate(ast: JsNode, sourceFile: File) {
        val expectedText = text.replace("\\n", System.lineSeparator())
        var elementExists = false
        object : RecursiveJsVisitor() {
            override fun visitElement(node: JsNode) {
                checkCommentExistsIn(node.getCommentsBeforeNode())
                checkCommentExistsIn(node.getCommentsAfterNode())
                if (elementExists) return
                super.visitElement(node)
            }

            override fun visitSingleLineComment(comment: JsSingleLineComment) {
                checkCommentExistsIn(listOf(comment))
            }

            override fun visitMultiLineComment(comment: JsMultiLineComment) {
                checkCommentExistsIn(listOf(comment))
            }

            fun checkCommentExistsIn(comments: List<JsComment>?) {
                if (comments == null) return
                for (comment in comments) {
                    if (isNeededCommentType(comment) && isTheSameText(comment.text, expectedText)) {
                        elementExists = true
                    }
                }
            }
        }.accept(ast)
        assertTrue(elementExists) {
            "${if (multiline) "Multi-line" else "Single-line"} comment with text '$text' doesn't exist"
        }
    }
}
