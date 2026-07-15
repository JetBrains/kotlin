// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package org.jetbrains.kotlin.js.backend.ast

/**
 * A JavaScript parameter.
 */
class JsParameter(
    declarable: JsDeclarable,
    defaultValue: JsExpression?,
    isRest: Boolean
) : SourceInfoAwareJsNode(), HasName {
    var declarable: JsDeclarable = declarable
        private set

    var defaultValue: JsExpression? = defaultValue
        private set

    var isRest: Boolean = isRest
        private set

    constructor(name: JsName) : this(JsDeclarable.Named(name), null, false)
    constructor(declarable: JsDeclarable.Named, isRest: Boolean) : this(declarable, null, isRest)
    constructor(declarable: JsDeclarable) : this(declarable, null, false)
    constructor(declarable: JsDeclarable, defaultValue: JsExpression?) : this(declarable, defaultValue, false)

    override fun getName() = (declarable as? HasName)?.name

    override fun setName(name: JsName?) {
        (declarable as? HasName)?.name = name
    }

    override fun accept(v: JsVisitor) {
        v.visitParameter(this)
    }

    override fun acceptChildren(v: JsVisitor) {
        v.accept(declarable)
        v.accept(defaultValue)
    }

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        if (v.visit(this, ctx)) {
            declarable = v.accept(declarable)
            defaultValue = v.accept(defaultValue)
        }
        v.endVisit(this, ctx)
    }

    override fun deepCopy(): JsParameter {
        return JsParameter(declarable.deepCopy(), defaultValue?.deepCopy(), isRest).withMetadataFrom(this)
    }
}
