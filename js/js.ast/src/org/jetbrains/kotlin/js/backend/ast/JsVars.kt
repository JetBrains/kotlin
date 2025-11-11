// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast

import com.intellij.util.SmartList
import org.jetbrains.kotlin.js.util.AstUtil

/**
 * A JavaScript variable(s) declaration statement.
 */
class JsVars : SourceInfoAwareJsNode, JsStatement, Iterable<JsVars.JsVar> {
    val vars: MutableList<JsVar>
    var isMultiline: Boolean
    val variant: Variant
    val isEmpty: Boolean
        get() = vars.isEmpty()

    constructor(variant: Variant) : this(variant, SmartList(), false)

    constructor(variant: Variant, multiline: Boolean) : this(variant, SmartList(), multiline)

    constructor(variant: Variant, vars: MutableList<JsVar>, multiline: Boolean) {
        this.variant = variant
        this.vars = vars
        this.isMultiline = multiline
    }

    constructor(variant: Variant, jsVar: JsVar) : this(variant, SmartList(jsVar), false)

    constructor(variant: Variant, vararg vars: JsVar) : this(variant, SmartList(*vars), false)

    fun add(jsVar: JsVar) {
        vars.add(jsVar)
    }

    fun addAll(vars: Collection<JsVar>) {
        this.vars.addAll(vars)
    }

    override fun iterator(): Iterator<JsVar> {
        return vars.iterator()
    }

    override fun accept(v: JsVisitor) {
        v.visitVars(this)
    }

    override fun acceptChildren(visitor: JsVisitor) {
        visitor.acceptWithInsertRemove(vars)
    }

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        if (v.visit(this, ctx)) {
            v.acceptList(vars)
        }
        v.endVisit(this, ctx)
    }

    override fun deepCopy(): JsVars {
        return JsVars(variant, AstUtil.deepCopy(vars), isMultiline).withMetadataFrom(this)
    }

    /**
     * A single variable-value binding.
     */
    class JsVar(
        private var name: JsName,
        var initExpression: JsExpression? = null
    ) : SourceInfoAwareJsNode(), HasName {
        override fun getName(): JsName {
            return this.name
        }

        override fun setName(name: JsName) {
            this.name = name
        }

        override fun accept(v: JsVisitor) {
            v.visit(this)
        }

        override fun acceptChildren(visitor: JsVisitor) {
            initExpression?.let { visitor.accept(it) }
        }

        override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
            if (v.visit(this, ctx)) {
                initExpression?.let {
                    initExpression = v.accept(it)
                }
            }
            v.endVisit(this, ctx)
        }

        override fun deepCopy(): JsVar {
            if (initExpression == null) return JsVar(name)

            return JsVar(this.name, initExpression?.deepCopy()).withMetadataFrom(this)
        }
    }

    enum class Variant {
        Var,
        Let,
        Const
    }
}