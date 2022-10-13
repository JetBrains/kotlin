/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

class JsImport(
    val module: String,
    val target: Target,
) : SourceInfoAwareJsNode(), JsStatement {
    constructor(module: String, elements: MutableList<Element> = mutableListOf()) : this(module, Target.Elements(elements))

    val elements: MutableList<Element>
        get() = (target as Target.Elements).elements

    sealed class Target {
        class Elements(val elements: MutableList<Element>) : Target()
        class Default(val name: JsName) : Target() {
            constructor(name: String) : this(JsName(name, false))
        }

        class All(val alias: JsName) : Target() {
            constructor(alias: String) : this(JsName(alias, false))
        }
    }

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
        JsImport(module, target)

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        v.visit(this, ctx)
        v.endVisit(this, ctx)
    }
}