/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

abstract class JsIterableLoop(
    val bindingVarVariant: JsVars.Variant?,
    val bindingVarName: JsName?,
    bindingExpression: JsExpression?,
    iterableExpression: JsExpression,
    body: JsStatement
) : SourceInfoAwareJsNode(), JsLoop {

    var bindingExpression: JsExpression? = bindingExpression
        private set

    var iterableExpression: JsExpression = iterableExpression
        private set

    var body: JsStatement = body
        private set

    abstract override fun accept(visitor: JsVisitor)

    override fun acceptChildren(visitor: JsVisitor) {
        bindingExpression?.let {
            visitor.acceptLvalue(it)
        }
        visitor.accept(iterableExpression)
        visitor.accept(body)
    }

    override fun traverse(
        visitor: JsVisitorWithContext,
        ctx: JsContext<*>,
    ) {
        if (visitor.visit(this, ctx)) {
            bindingExpression?.let {
                bindingExpression = visitor.acceptLvalue(bindingExpression)
            }
            iterableExpression = visitor.acceptLvalue(iterableExpression)
            body = visitor.acceptStatement(body)
        }
        visitor.endVisit(this, ctx)
    }

    abstract override fun deepCopy(): JsStatement
}