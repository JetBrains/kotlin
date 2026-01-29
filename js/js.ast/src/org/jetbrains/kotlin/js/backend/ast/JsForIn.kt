// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast

class JsForIn(
    bindingVarVariant: JsVars.Variant?,
    private val bindingVarName: JsAssignable.Named?,
    bindingExpression: JsExpression?,
    iterableExpression: JsExpression,
    body: JsStatement,
) : JsIterableLoop(bindingVarVariant, bindingVarName, bindingExpression, iterableExpression, body) {
    override fun accept(visitor: JsVisitor) {
        visitor.visitForIn(this)
    }

    override fun deepCopy(): JsStatement {
        return JsForIn(
            bindingVarVariant,
            bindingVarName,
            bindingExpression?.deepCopy(),
            iterableExpression.deepCopy(),
            body.deepCopy()
        ).withMetadataFrom(this)
    }
}