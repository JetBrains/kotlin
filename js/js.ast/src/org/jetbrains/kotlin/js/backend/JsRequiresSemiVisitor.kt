/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend

import org.jetbrains.kotlin.js.backend.ast.*

/**
 * Determines if a statement at the end of a block requires a semicolon.
 *
 * For example, the following statements require semicolons:
 * - `if (cond);`
 * - `while (cond);`
 *
 * The following do not require semicolons:
 * - `return 1`
 * - `do {} while(true)`
 */
class JsRequiresSemiVisitor private constructor() : JsVisitor() {
    private var needsSemicolon = false

    override fun visitFor(x: JsFor) {
        if (x.body is JsEmpty) {
            needsSemicolon = true
        }
    }

    override fun visitIterableLoop(x: JsIterableLoop) {
        if (x.body is JsEmpty) {
            needsSemicolon = true
        }
    }

    override fun visitIf(x: JsIf) {
        val thenStmt = x.thenStatement
        val elseStmt = x.elseStatement
        val toCheck = elseStmt ?: thenStmt
        if (toCheck is JsEmpty) {
            needsSemicolon = true
        } else {
            // Must recurse to determine last statement (possible if-else chain).
            accept(toCheck)
        }
    }

    override fun visitLabel(x: JsLabel) {
        if (x.statement is JsEmpty) {
            needsSemicolon = true
        }
    }

    override fun visitWhile(x: JsWhile) {
        if (x.body is JsEmpty) {
            needsSemicolon = true
        }
    }

    companion object {
        fun exec(lastStatement: JsStatement): Boolean {
            val visitor = JsRequiresSemiVisitor()
            visitor.accept(lastStatement)
            return visitor.needsSemicolon
        }
    }
}
