/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

class JsBindingProperty(
    propertyName: JsExpression?,
    element: JsBindingElement,
) : SourceInfoAwareJsNode() {
    var propertyName: JsExpression? = propertyName
        private set
    var element: JsBindingElement = element
        private set

    override fun accept(visitor: JsVisitor) {
        visitor.visitBindingProperty(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
        propertyName?.accept(visitor)
        element.accept(visitor)
    }

    override fun traverse(
        visitor: JsVisitorWithContext,
        ctx: JsContext<*>,
    ) {
        if (visitor.visit(this, ctx)) {
            propertyName = visitor.accept(propertyName)
            element = visitor.accept(element)
        }
        visitor.endVisit(this, ctx)
    }

    override fun deepCopy(): JsBindingProperty {
        return JsBindingProperty(propertyName?.deepCopy(), element.deepCopy()).withMetadataFrom(this)
    }
}