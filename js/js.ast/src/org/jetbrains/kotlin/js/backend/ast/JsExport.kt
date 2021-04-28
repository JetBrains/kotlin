/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

class JsExport(
    val subject: Subject,
    val fromModule: String? = null,
) : SourceInfoAwareJsNode(), JsStatement {

    constructor(element: Element) : this(Subject.Elements(listOf(element)))
    constructor(name: JsName, alias: JsName? = null) : this(Element(name, alias))

    sealed class Subject {
        class Elements(val elements: List<Element>) : Subject()
        object All : Subject()
    }

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
        JsExport(subject, fromModule)

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        v.visit(this, ctx)
        v.endVisit(this, ctx)
    }
}