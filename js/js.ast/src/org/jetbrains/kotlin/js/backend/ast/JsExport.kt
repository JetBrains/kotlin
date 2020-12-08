/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

class JsExport(
    val elements: List<Element>,
) : SourceInfoAwareJsNode(), JsStatement {

    class Element(
        val name: JsName,
        val alias: JsName?
    )

    override fun accept(visitor: JsVisitor) {
        visitor.visitExport(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
    }

    override fun deepCopy(): JsStatement =
        JsExport(elements.map { it })

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        v.visit(this, ctx)
        v.endVisit(this, ctx)
    }
}