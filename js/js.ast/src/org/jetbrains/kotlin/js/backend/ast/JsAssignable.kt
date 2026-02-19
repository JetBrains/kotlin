/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

sealed class JsAssignable : SourceInfoAwareJsNode() {
    abstract override fun deepCopy(): JsAssignable

    val names: List<JsName>
        get() = mutableListOf<JsName>().apply {
            accept(NamesCollector(this))
        }

    class Named(private var name: JsName) : JsAssignable(), HasName {
        override fun getName(): JsName = name

        override fun setName(name: JsName) {
            this.name = name
        }

        override fun accept(visitor: JsVisitor) {
            visitor.visitNamedAssignable(this)
        }

        override fun deepCopy(): Named {
            return Named(name).withMetadataFrom(this)
        }

        override fun traverse(
            visitor: JsVisitorWithContext,
            ctx: JsContext<*>,
        ) {
            visitor.visit(this, ctx)
            visitor.endVisit(this, ctx)
        }
    }

    class ArrayPattern(val elements: List<JsBindingArrayItem>) : JsAssignable() {
        override fun accept(visitor: JsVisitor) {
            visitor.visitArrayPatternAssignable(this)
        }

        override fun acceptChildren(visitor: JsVisitor) {
            visitor.acceptList(elements)
        }

        override fun traverse(
            visitor: JsVisitorWithContext,
            ctx: JsContext<*>,
        ) {
            if (visitor.visit(this, ctx)) {
                visitor.acceptList(elements)
            }
            visitor.endVisit(this, ctx)
        }

        override fun deepCopy(): JsAssignable {
            return ArrayPattern(elements.map { it.deepCopy() }).withMetadataFrom(this)
        }
    }

    class ObjectPattern(val properties: List<JsBindingProperty>) : JsAssignable() {
        override fun accept(visitor: JsVisitor) {
            visitor.visitObjectPatternAssignable(this)
        }

        override fun acceptChildren(visitor: JsVisitor) {
            visitor.acceptList(properties)
        }

        override fun traverse(
            visitor: JsVisitorWithContext,
            ctx: JsContext<*>,
        ) {
            if (visitor.visit(this, ctx)) {
                visitor.acceptList(properties)
            }
            visitor.endVisit(this, ctx)
        }

        override fun deepCopy(): JsAssignable {
            return ObjectPattern(properties.map { it.deepCopy() }).withMetadataFrom(this)
        }
    }

    private class NamesCollector(private val names: MutableList<JsName>) : RecursiveJsVisitor() {
        override fun visitNamedAssignable(assignable: Named) {
            names.add(assignable.name)
            super.visitNamedAssignable(assignable)
        }
    }
}