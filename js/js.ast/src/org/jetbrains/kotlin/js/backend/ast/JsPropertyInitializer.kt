// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package org.jetbrains.kotlin.js.backend.ast

/**
 * Used in object literals to specify properties.
 */
sealed class JsPropertyInitializer : SourceInfoAwareJsNode() {
    class KeyValue(
        labelExpr: JsExpression,
        valueExpr: JsExpression,
    ) : JsPropertyInitializer() {
        var labelExpr: JsExpression = labelExpr
            private set
        var valueExpr: JsExpression = valueExpr
            private set

        override fun accept(v: JsVisitor) {
            v.visitKeyValuePropertyInitializer(this)
        }

        override fun acceptChildren(visitor: JsVisitor) {
            visitor.accept(labelExpr)
            visitor.accept(valueExpr)
        }

        override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
            if (v.visit(this, ctx)) {
                labelExpr = v.accept(labelExpr)
                valueExpr = v.accept(valueExpr)
            }
            v.endVisit(this, ctx)
        }

        override fun deepCopy(): KeyValue {
            return KeyValue(
                labelExpr.deepCopy(),
                valueExpr.deepCopy()
            ).withMetadataFrom<KeyValue>(this)
        }

        override fun toString() = "$labelExpr: $valueExpr"
    }

    class Spread(expression: JsExpression) : JsPropertyInitializer() {
        var expression: JsExpression = expression
            private set

        override fun accept(v: JsVisitor) {
            v.visitSpreadPropertyInitializer(this)
        }

        override fun acceptChildren(visitor: JsVisitor) {
            visitor.accept(expression)
        }

        override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
            if (v.visit(this, ctx)) {
                expression = v.accept(expression)
            }
            v.endVisit(this, ctx)
        }

        override fun deepCopy(): Spread {
            return Spread(
                expression.deepCopy(),
            ).withMetadataFrom<Spread>(this)
        }

        override fun toString() = "...$expression"
    }
}
