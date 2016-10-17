/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast

import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.internal.log
import org.jetbrains.uast.visitor.UastVisitor

class UNamedExpression(
        override val name: String,
        override val containingElement: UElement?
): UExpression, UNamed {
    lateinit var expression: UExpression

    override val annotations: List<UAnnotation>
        get() = emptyList()

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitElement(this)) return
        annotations.acceptList(visitor)
        expression.accept(visitor)
        visitor.afterVisitElement(this)
    }

    override fun asLogString() = log("UNamedExpression ($name)", expression)
    override fun asRenderString() = name + " = " + expression.asRenderString()

    override fun evaluate() = expression.evaluate()
    
    companion object {
        inline fun create(name: String, parent: UElement?, innerExpr: UElement.() -> UExpression): UNamedExpression {
            return UNamedExpression(name, parent).apply { 
                expression = innerExpr(this)
            }
        }
    }
}
