/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

import com.intellij.util.SmartList
import org.jetbrains.kotlin.js.util.AstUtil

/**
 * Represents a JavaScript block statement.
 */
open class JsBlock private constructor(statements: SmartList<JsStatement>) : SourceInfoAwareJsNode(), JsStatement, JsLexicalScope {
    val statements: MutableList<JsStatement> = statements

    constructor(statements: List<JsStatement>) : this(SmartList(statements))
    constructor(statement: JsStatement) : this(SmartList(statement))
    constructor(vararg statements: JsStatement) : this(SmartList(*statements))
    constructor() : this(SmartList())

    var closingBraceSource: JsLocationWithSource? = null

    val isEmpty: Boolean
        get() = statements.isEmpty()

    override fun accept(v: JsVisitor) {
        v.visitBlock(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
        visitor.acceptWithInsertRemove(statements)
    }

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        if (v.visit(this, ctx)) {
            v.acceptStatementList(statements)
        }
        v.endVisit(this, ctx)
    }

    override fun deepCopy(): JsBlock {
        val block = JsBlock(AstUtil.deepCopy(statements)).withMetadataFrom(this)
        block.closingBraceSource = closingBraceSource
        return block
    }
}
