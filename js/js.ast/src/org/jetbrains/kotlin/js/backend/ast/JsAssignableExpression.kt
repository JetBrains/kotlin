/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.backend.ast

/**
 * A marker for expressions that may appear on the left-hand side of an assignment (an l-value / reference),
 * such as [JsNameRef] (`a`, `a.b`), [JsArrayAccess] (`a[i]`) and [JsThisRef] (`this`).
 *
 * This is the type of [JsAssignmentOperation.Simple]'s target, so the AST statically guarantees that a
 * regular assignment writes to an assignable expression rather than to an arbitrary expression.
 */
abstract class JsAssignableExpression : JsExpression() {
    abstract override fun deepCopy(): JsAssignableExpression
}
