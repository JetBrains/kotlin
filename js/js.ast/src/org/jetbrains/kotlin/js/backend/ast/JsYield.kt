/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

import org.jetbrains.kotlin.js.util.AstUtil

class JsYield(var expression: JsExpression?) : JsExpression() {
    override fun accept(visitor: JsVisitor) {
        visitor.visitYield(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
        visitor.accept(expression)
    }

    override fun deepCopy(): JsExpression {
        return JsYield(AstUtil.deepCopy(expression)).withMetadataFrom(this)
    }

    override fun traverse(visitor: JsVisitorWithContext, ctx: JsContext<*>) {
        if (visitor.visit(this, ctx)) {
            expression = visitor.accept(expression)
        }
        visitor.endVisit(this, ctx)
    }
}