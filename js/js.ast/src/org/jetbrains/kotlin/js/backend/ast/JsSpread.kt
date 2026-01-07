/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

class JsSpread(expression: JsExpression) : JsExpression() {
    var expression: JsExpression = expression
        private set

    override fun accept(visitor: JsVisitor) {
        visitor.visitSpread(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
        visitor.accept(expression)
    }

    override fun traverse(
        visitor: JsVisitorWithContext,
        ctx: JsContext<*>,
    ) {
        if (visitor.visit(this, ctx)) {
            expression = visitor.accept(expression)
        }
        visitor.endVisit(this, ctx)
    }

    override fun deepCopy(): JsExpression {
        return JsSpread(expression.deepCopy()).withMetadataFrom(this)
    }
}