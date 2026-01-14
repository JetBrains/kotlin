// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package org.jetbrains.kotlin.js.backend.ast

/**
 * A JavaScript parameter.
 */
class JsParameter(
    private var name: JsName,
    defaultValue: JsExpression?,
    isRest: Boolean
) : SourceInfoAwareJsNode(), HasName {
    var defaultValue: JsExpression? = defaultValue
        private set

    var isRest: Boolean = isRest
        private set

    constructor(name: JsName) : this(name, null, false)
    constructor(name: JsName, isRest: Boolean) : this(name, null, isRest)
    constructor(name: JsName, defaultValue: JsExpression?) : this(name, defaultValue, false)

    override fun getName() = name

    override fun setName(name: JsName) {
        this.name = name
    }

    override fun accept(v: JsVisitor) {
        v.visitParameter(this)
    }

    override fun acceptChildren(v: JsVisitor) {
        v.accept(defaultValue)
    }

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        if (v.visit(this, ctx)) {
            defaultValue = v.accept(defaultValue)
        }
        v.endVisit(this, ctx)
    }

    override fun deepCopy(): JsParameter {
        return JsParameter(name, defaultValue?.deepCopy(), isRest).withMetadataFrom(this)
    }
}
