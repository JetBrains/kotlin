/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.utils

import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.RecursiveJsVisitor
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

class PropertyReferenceCollector : RecursiveJsVisitor() {

    private val identReadMap = hashMapOf<String, Int>()
    private val identWriteMap = hashMapOf<String, Int>()

    fun hasUnqualifiedReads(expectedIdent: String) = expectedIdent in identReadMap
    fun hasUnqualifiedWrites(expectedIdent: String) = expectedIdent in identWriteMap

    fun unqualifiedWriteCount(expectedIdent: String): Int = identWriteMap[expectedIdent] ?: 0

    fun unqualifiedReadCount(expectedIdent: String): Int = identReadMap[expectedIdent] ?: 0

    override fun visitNameRef(nameRef: JsNameRef) {
        super.visitNameRef(nameRef)
        identReadMap[nameRef.ident] = 1 + unqualifiedReadCount(nameRef.ident)
    }

    override fun visitBinaryExpression(x: JsBinaryOperation) {
        var assignmentToProperty = false
        JsAstUtils.decomposeAssignment(x)?.let { (left, right) ->
            (left as? JsNameRef)?.let { nameRef ->
                assignmentToProperty = true
                identWriteMap[nameRef.ident] = 1 + unqualifiedWriteCount(nameRef.ident)
                nameRef.qualifier?.accept(this)
                right.accept(this)
            }
        }
        if (!assignmentToProperty) {
            super.visitBinaryExpression(x)
        }
    }

    companion object {
        fun collect(node: JsNode): PropertyReferenceCollector {
            val visitor = PropertyReferenceCollector()
            node.accept(visitor)
            return visitor
        }
    }
}
