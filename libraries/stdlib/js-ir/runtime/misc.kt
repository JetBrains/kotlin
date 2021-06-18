/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

// TODO: Polyfill
@OptIn(JsIntrinsic::class)
internal fun imul(a_local: Int, b_local: Int): Int {
    val lhs = jsBitAnd(a_local, js("0xffff0000")).toDouble() * jsBitAnd(b_local, 0xffff).toDouble()
    val rhs = jsBitAnd(a_local, 0xffff).toDouble() * b_local.toDouble()
    return jsBitOr(lhs + rhs, 0)
}
