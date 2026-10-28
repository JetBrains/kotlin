/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

import org.jetbrains.kotlin.js.util.AstUtil

/**
 * A JavaScript switch statement.
 */
class JsSwitch(
    var expression: JsExpression,
    cases: List<JsSwitchMember> = emptyList(),
) : SourceInfoAwareJsNode(), JsStatement, JsLexicalScope {
    val cases: MutableList<JsSwitchMember> = cases.toMutableList()

    override fun accept(v: JsVisitor) {
        v.visit(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
        visitor.accept(expression)
        visitor.acceptWithInsertRemove(cases)
    }

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        if (v.visit(this, ctx)) {
            expression = v.accept(expression)
            v.acceptList(cases)
        }
        v.endVisit(this, ctx)
    }

    override fun deepCopy(): JsSwitch {
        val expressionCopy = AstUtil.deepCopy(expression)!!
        val casesCopy = AstUtil.deepCopy(cases)

        return JsSwitch(expressionCopy, casesCopy).withMetadataFrom(this)
    }
}
