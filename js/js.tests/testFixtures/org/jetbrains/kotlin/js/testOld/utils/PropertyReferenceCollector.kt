/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.utils

import org.jetbrains.kotlin.js.backend.ast.JsAssignmentOperation
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.RecursiveJsVisitor

class PropertyReferenceCollector : RecursiveJsVisitor() {

    private val identReadMap = hashMapOf<String, Int>()
    private val identWriteMap = hashMapOf<String, Int>()

    fun unqualifiedWriteCount(expectedIdent: String): Int = identWriteMap[expectedIdent] ?: 0

    fun unqualifiedReadCount(expectedIdent: String): Int = identReadMap[expectedIdent] ?: 0

    override fun visitNameRef(nameRef: JsNameRef) {
        super.visitNameRef(nameRef)
        identReadMap[nameRef.ident] = 1 + unqualifiedReadCount(nameRef.ident)
    }

    override fun visitSimpleAssignment(x: JsAssignmentOperation.Simple) {
        val left = x.target
        val right = x.value
        val nameRef = left as? JsNameRef
        if (nameRef != null) {
            identWriteMap[nameRef.ident] = 1 + unqualifiedWriteCount(nameRef.ident)
            nameRef.qualifier?.accept(this)
            right.accept(this)
        } else {
            super.visitSimpleAssignment(x)
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
