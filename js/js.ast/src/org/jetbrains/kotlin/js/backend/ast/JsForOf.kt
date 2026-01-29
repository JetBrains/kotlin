/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

class JsForOf(
    bindingVarVariant: JsVars.Variant?,
    bindingAssignable: JsAssignable?,
    bindingExpression: JsExpression?,
    iterableExpression: JsExpression,
    body: JsStatement,
) : JsIterableLoop(bindingVarVariant, bindingAssignable, bindingExpression, iterableExpression, body) {
    override fun accept(visitor: JsVisitor) {
        visitor.visitForOf(this)
    }

    override fun deepCopy(): JsStatement {
        return JsForOf(
            bindingVarVariant,
            bindingAssignable?.deepCopy(),
            bindingExpression?.deepCopy(),
            iterableExpression.deepCopy(),
            body.deepCopy()
        ).withMetadataFrom(this)
    }
}