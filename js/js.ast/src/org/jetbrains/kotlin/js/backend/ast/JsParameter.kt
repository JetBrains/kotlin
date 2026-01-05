// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package org.jetbrains.kotlin.js.backend.ast

/**
 * A JavaScript parameter.
 */
class JsParameter(
    private var name: JsName,
    isRest: Boolean
) : SourceInfoAwareJsNode(), HasName {
    var isRest: Boolean = isRest
        private set

    constructor(name: JsName) : this(name, false)

    override fun getName() = name

    override fun setName(name: JsName) {
        this.name = name
    }

    override fun accept(v: JsVisitor) {
        v.visitParameter(this)
    }

    override fun traverse(v: JsVisitorWithContext, ctx: JsContext<*>) {
        v.visit(this, ctx)
        v.endVisit(this, ctx)
    }

    override fun deepCopy(): JsParameter {
        return JsParameter(name, isRest).withMetadataFrom(this)
    }
}
