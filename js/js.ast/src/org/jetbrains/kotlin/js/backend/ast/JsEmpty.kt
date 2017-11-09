// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast

object JsEmpty : SourceInfoAwareJsNode(), JsStatement {

    override fun accept(v: JsVisitor) {
        v.visitEmpty(this)
    }

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        v.visit(this, ctx)
        v.endVisit(this, ctx)
    }

    override fun deepCopy(): JsEmpty {
        return this
    }
}
