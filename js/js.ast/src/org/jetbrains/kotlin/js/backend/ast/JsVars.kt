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
        var declarable: JsDeclarable,
        var initExpression: JsExpression? = null
    ) : SourceInfoAwareJsNode(), HasName {
        constructor(name: JsName, initExpression: JsExpression? = null) : this(JsDeclarable.Named(name), initExpression)

        override fun getName() = (declarable as? HasName)?.name

        override fun setName(name: JsName?) {
            (declarable as? HasName)?.name = name
        }

        override fun accept(v: JsVisitor) {
            v.visit(this)
        }

        override fun acceptChildren(visitor: JsVisitor) {
            visitor.accept(declarable)
            visitor.accept(initExpression)
        }

        override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
            if (v.visit(this, ctx)) {
                declarable = v.accept(declarable)
                initExpression = v.accept(initExpression)
            }
            v.endVisit(this, ctx)
        }

        override fun deepCopy(): JsVar {
            if (initExpression == null) return JsVar(declarable)

            return JsVar(declarable.deepCopy(), initExpression?.deepCopy()).withMetadataFrom(this)
        }
    }

    enum class Variant {
        Var,
        Let,
        Const
    }
}
