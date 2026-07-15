/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

sealed class JsDeclarable : SourceInfoAwareJsNode() {
    abstract override fun deepCopy(): JsDeclarable

    val names: List<JsName>
        get() = mutableListOf<JsName>().apply {
            accept(NamesCollector(this))
        }

    class Named(private var name: JsName) : JsDeclarable(), HasName {
        override fun getName(): JsName = name

        override fun setName(name: JsName) {
            this.name = name
        }

        override fun accept(visitor: JsVisitor) {
            visitor.visitNamedDeclarable(this)
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

    class ArrayPattern(val elements: List<JsBindingArrayItem>) : JsDeclarable() {
        override fun accept(visitor: JsVisitor) {
            visitor.visitArrayPatternDeclarable(this)
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

        override fun deepCopy(): JsDeclarable {
            return ArrayPattern(elements.map { it.deepCopy() }).withMetadataFrom(this)
        }
    }

    class ObjectPattern(val properties: List<JsBindingProperty>) : JsDeclarable() {
        override fun accept(visitor: JsVisitor) {
            visitor.visitObjectPatternDeclarable(this)
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

        override fun deepCopy(): JsDeclarable {
            return ObjectPattern(properties.map { it.deepCopy() }).withMetadataFrom(this)
        }
    }

    private class NamesCollector(private val names: MutableList<JsName>) : RecursiveJsVisitor() {
        override fun visitNamedDeclarable(declarable: Named) {
            names.add(declarable.name)
            super.visitNamedDeclarable(declarable)
        }
    }
}
