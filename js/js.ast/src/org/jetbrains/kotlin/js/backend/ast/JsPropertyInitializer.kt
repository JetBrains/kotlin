// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package org.jetbrains.kotlin.js.backend.ast

/**
 * Used in object literals to specify properties.
 */
open class JsPropertyInitializer(
    labelExpr: JsExpression,
    valueExpr: JsExpression
) : SourceInfoAwareJsNode() {
    var labelExpr: JsExpression = labelExpr
        protected set
    var valueExpr: JsExpression = valueExpr
        protected set

    override fun accept(v: JsVisitor) {
        v.visitPropertyInitializer(this)
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

    override fun deepCopy(): JsPropertyInitializer {
        return JsPropertyInitializer(
            labelExpr.deepCopy(),
            valueExpr.deepCopy()
        ).withMetadataFrom<JsPropertyInitializer>(this)
    }

    override fun toString() = "$labelExpr: $valueExpr"
}
