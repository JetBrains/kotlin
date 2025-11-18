// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast

import org.jetbrains.kotlin.js.util.AstUtil

class JsForIn(
    bindingVarVariant: JsVars.Variant?,
    bindingVarName: JsName?,
    bindingExpression: JsExpression?,
    iterableExpression: JsExpression,
    body: JsStatement,
) : JsIterableLoop(bindingVarVariant, bindingVarName, bindingExpression, iterableExpression, body) {
    override fun accept(visitor: JsVisitor) {
        visitor.visitForIn(this)
    }

    override fun deepCopy(): JsStatement {
        val bindingExprCopy = AstUtil.deepCopy(bindingExpression)
        val iterableExprCopy = AstUtil.deepCopy(iterableExpression) ?: error("Non-nullable iterable expected")
        val bodyCopy = AstUtil.deepCopy(body) ?: error("Non-nullable body expected")

        return JsForIn(bindingVarVariant, bindingVarName, bindingExprCopy, iterableExprCopy, bodyCopy).withMetadataFrom(this)
    }
}