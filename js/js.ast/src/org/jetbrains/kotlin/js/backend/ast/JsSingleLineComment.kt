/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

class JsSingleLineComment(override val text: String) : SourceInfoAwareJsNode(), JsComment {
    override fun accept(visitor: JsVisitor) {
        visitor.visitSingleLineComment(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
    }

    override fun traverse(visitor: JsVisitorWithContext, ctx: JsContext<*>) {
        visitor.visit(this, ctx)
        visitor.endVisit(this, ctx)
    }

    override fun deepCopy() = this
}