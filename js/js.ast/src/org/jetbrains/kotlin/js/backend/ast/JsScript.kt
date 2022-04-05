/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

import org.jetbrains.kotlin.js.util.AstUtil

class JsScript(val statements: List<JsStatement>, val comments: List<JsComment>) : JsStatement, JsExpression() {
    constructor(comments: List<JsComment>) : this(mutableListOf(), comments)

    override fun deepCopy(): JsScript {
        return JsScript(AstUtil.deepCopy(statements), comments).withMetadataFrom(this);
    }

    override fun accept(v: JsVisitor) {
        v.visitScript(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
        visitor.acceptList(statements)
    }

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        if (v.visit(this, ctx)) {
            v.acceptStatementList(statements)
        }
        v.endVisit(this, ctx)
    }
}