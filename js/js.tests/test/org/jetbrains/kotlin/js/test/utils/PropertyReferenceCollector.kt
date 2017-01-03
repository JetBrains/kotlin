/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.test.utils

import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.RecursiveJsVisitor
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

class PropertyReferenceCollector : RecursiveJsVisitor() {

    private val identReadSet = hashSetOf<String>()
    private val identWriteSet = hashSetOf<String>()

    fun hasUnqualifiedReads(expectedIdent: String) = expectedIdent in identReadSet
    fun hasUnqualifiedWrites(expectedIdent: String) = expectedIdent in identWriteSet

    override fun visitNameRef(nameRef: JsNameRef) {
        super.visitNameRef(nameRef)
        identReadSet.add(nameRef.ident)
    }

    override fun visitBinaryExpression(x: JsBinaryOperation) {
        var assignmentToProperty = false
        JsAstUtils.decomposeAssignment(x)?.let { (left, right) ->
            (left as? JsNameRef)?.let { nameRef ->
                assignmentToProperty = true
                identWriteSet.add(nameRef.ident)
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
