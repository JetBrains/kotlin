/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

import org.jetbrains.kotlin.js.util.AstUtil

class JsForOf(
    bindingVarVariant: JsVars.Variant?,
    bindingVarName: JsName?,
    bindingExpression: JsExpression?,
    iterableExpression: JsExpression,
    body: JsStatement,
) : JsIterableLoop(bindingVarVariant, bindingVarName, bindingExpression, iterableExpression, body) {
    override fun accept(visitor: JsVisitor) {
        visitor.visitForOf(this)
    }

    override fun deepCopy(): JsStatement {
        val bindingExprCopy = AstUtil.deepCopy(bindingExpression)
        val iterableExprCopy = AstUtil.deepCopy(iterableExpression) ?: error("Non-nullable iterable expected")
        val bodyCopy = AstUtil.deepCopy(body) ?: error("Non-nullable body expected")

        return JsForOf(bindingVarVariant, bindingVarName, bindingExprCopy, iterableExprCopy, bodyCopy).withMetadataFrom(this)
    }
}