/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

class JsBindingElement(
    target: JsAssignable,
    defaultValue: JsExpression?,
    val isSpread: Boolean
) : SourceInfoAwareJsNode() {
    var target: JsAssignable = target
        private set

    var defaultValue: JsExpression? = defaultValue
        private set

    override fun accept(visitor: JsVisitor) {
        visitor.visitBindingElement(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
        target.accept(visitor)
        defaultValue?.accept(visitor)
    }

    override fun traverse(
        visitor: JsVisitorWithContext,
        ctx: JsContext<*>,
    ) {
        if (visitor.visit(this, ctx)) {
            target = visitor.accept(target)
            defaultValue = visitor.accept(defaultValue)
        }
        visitor.endVisit(this, ctx)
    }

    override fun deepCopy(): JsBindingElement {
        return JsBindingElement(target.deepCopy(), defaultValue?.deepCopy(), isSpread).withMetadataFrom(this)
    }
}