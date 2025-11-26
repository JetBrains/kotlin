/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

class JsTemplateStringLiteral(
    var tag: JsExpression?,
    val segments: List<Segment>,
) : JsLiteral() {
    override fun accept(visitor: JsVisitor) {
        visitor.visitTemplateString(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
        visitor.accept(tag)
        visitor.acceptList(segments)
    }

    override fun traverse(
        visitor: JsVisitorWithContext,
        ctx: JsContext<*>,
    ) {
        if (visitor.visit(this, ctx)) {
            tag = visitor.accept(tag)
            visitor.acceptList(segments)
        }
        visitor.endVisit(this, ctx)
    }

    override fun deepCopy(): JsTemplateStringLiteral =
        JsTemplateStringLiteral(
            tag?.deepCopy(),
            segments.map { it.deepCopy() }
        ).withMetadataFrom(this)

    sealed class Segment : JsLiteral() {
        abstract override fun deepCopy(): Segment

        class StringLiteral(val value: String) : Segment() {
            override fun isLeaf() = true

            override fun accept(visitor: JsVisitor) {
                visitor.visitTemplateSegmentString(this)
            }

            override fun traverse(
                visitor: JsVisitorWithContext,
                ctx: JsContext<*>,
            ) {
                visitor.visit(this, ctx)
                visitor.endVisit(this, ctx)
            }

            override fun deepCopy(): StringLiteral {
                return StringLiteral(value)
            }
        }

        class Interpolation(expression: JsExpression) : Segment() {
            var expression = expression
                private set

            override fun accept(visitor: JsVisitor) {
                visitor.visitTemplateSegmentInterpolation(this)
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

            override fun deepCopy(): Interpolation {
                return Interpolation(expression.deepCopy())
            }
        }
    }
}