/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

sealed class JsBindingArrayItem : SourceInfoAwareJsNode() {
    abstract override fun deepCopy(): JsBindingArrayItem

    class Element(element: JsBindingElement) : JsBindingArrayItem() {
        var element: JsBindingElement = element
            private set

        override fun accept(visitor: JsVisitor) {
            visitor.visitBindingArrayItemElement(this)
        }

        override fun acceptChildren(visitor: JsVisitor) {
            element.accept(visitor)
        }

        override fun traverse(
            visitor: JsVisitorWithContext,
            ctx: JsContext<*>,
        ) {
            if (visitor.visit(this, ctx)) {
                element = visitor.accept(element)
            }
            visitor.endVisit(this, ctx)
        }

        override fun deepCopy(): JsBindingArrayItem {
            return Element(element.deepCopy()).withMetadataFrom(this)
        }
    }

    class Hole : JsBindingArrayItem() {
        override fun accept(visitor: JsVisitor) {
            visitor.visitBindingArrayItemHole(this)
        }

        override fun traverse(
            visitor: JsVisitorWithContext,
            ctx: JsContext<*>,
        ) {
            visitor.visit(this, ctx)
            visitor.endVisit(this, ctx)
        }

        override fun deepCopy(): JsBindingArrayItem {
            return Hole().withMetadataFrom(this)
        }
    }
}