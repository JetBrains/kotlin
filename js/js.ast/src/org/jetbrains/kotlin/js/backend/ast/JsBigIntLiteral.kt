/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast

import java.math.BigInteger

class JsBigIntLiteral(val value: BigInteger) : JsLiteral.JsValueLiteral() {
    constructor(value: Long) : this(BigInteger.valueOf(value))

    override fun accept(visitor: JsVisitor) {
        visitor.visitBigInt(this)
    }

    override fun toString(): String = value.toString()

    override fun traverse(visitor: JsVisitorWithContext, ctx: JsContext<*>) {
        visitor.visit(this, ctx)
        visitor.endVisit(this, ctx)
    }

    override fun deepCopy(): JsExpression {
        return JsBigIntLiteral(value).withMetadataFrom(this)
    }
}
