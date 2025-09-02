/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

class JsExpressionSequence(expressions: List<JsExpression>) : JsExpression() {
    override fun deepCopy(): JsExpression {
        TODO("Not yet implemented")
    }

    override fun accept(visitor: JsVisitor?) {
        TODO("Not yet implemented")
    }

    override fun traverse(
        visitor: JsVisitorWithContext?,
        ctx: JsContext<*>?,
    ) {
        TODO("Not yet implemented")
    }
}