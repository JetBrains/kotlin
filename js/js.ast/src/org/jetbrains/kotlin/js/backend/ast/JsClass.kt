/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

import org.jetbrains.kotlin.js.common.Symbol

class JsClass(
    private var name: JsName? = null,
    var baseClass: JsExpression? = null,
    var constructor: JsFunction? = null,
    val members: MutableList<JsFunction> = mutableListOf()
) : JsLiteral(), HasName {
    override fun getName(): JsName? {
        return name
    }

    override fun getSymbol(): Symbol? {
        return name
    }

    override fun setName(name: JsName?) {
        this.name = name
    }

    override fun accept(v: JsVisitor) {
        v.visitClass(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
        visitor.accept(baseClass)
        visitor.accept(constructor)
        visitor.acceptList(members)
    }

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        if (v.visit(this, ctx)) {
            baseClass = v.accept(baseClass)
            constructor = v.accept(constructor)
            v.acceptList(members)
        }
        v.endVisit(this, ctx)
    }

    override fun deepCopy(): JsClass {
        val classCopy = JsClass(name, baseClass, constructor?.deepCopy(), members.mapTo(mutableListOf()) { it.deepCopy() })

        return classCopy.withMetadataFrom<JsClass>(this)
    }
}
