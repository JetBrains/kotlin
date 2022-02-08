/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

class JsImport(
    val module: String,
    val elements: MutableList<Element> = mutableListOf(),
) : SourceInfoAwareJsNode(), JsStatement {

    class Element(
        val name: JsName,
        val alias: JsName?
    ) {
        constructor(name: String, alias: String?) : this(JsName(name, false), alias?.let { JsName(it, false) })
    }

    override fun accept(visitor: JsVisitor) {
        visitor.visitImport(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
    }

    override fun deepCopy(): JsStatement =
        JsImport(module, elements.map { it }.toMutableList())

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        v.visit(this, ctx)
        v.endVisit(this, ctx)
    }
}