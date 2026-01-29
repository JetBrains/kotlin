// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package org.jetbrains.kotlin.js.backend.ast

/**
 * A JavaScript parameter.
 */
class JsParameter(
    assignable: JsAssignable,
    defaultValue: JsExpression?,
    isRest: Boolean
) : SourceInfoAwareJsNode(), HasName {
    var assignable: JsAssignable = assignable
        private set

    var defaultValue: JsExpression? = defaultValue
        private set

    var isRest: Boolean = isRest
        private set

    constructor(name: JsName) : this(JsAssignable.Named(name), null, false)
    constructor(assignable: JsAssignable.Named, isRest: Boolean) : this(assignable, null, isRest)
    constructor(assignable: JsAssignable) : this(assignable, null, false)
    constructor(assignable: JsAssignable, defaultValue: JsExpression?) : this(assignable, defaultValue, false)

    override fun getName() = (assignable as? HasName)?.name

    override fun setName(name: JsName?) {
        (assignable as? HasName)?.name = name
    }

    override fun accept(v: JsVisitor) {
        v.visitParameter(this)
    }

    override fun acceptChildren(v: JsVisitor) {
        v.accept(assignable)
        v.accept(defaultValue)
    }

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        if (v.visit(this, ctx)) {
            assignable = v.accept(assignable)
            defaultValue = v.accept(defaultValue)
        }
        v.endVisit(this, ctx)
    }

    override fun deepCopy(): JsParameter {
        return JsParameter(assignable.deepCopy(), defaultValue?.deepCopy(), isRest).withMetadataFrom(this)
    }
}
