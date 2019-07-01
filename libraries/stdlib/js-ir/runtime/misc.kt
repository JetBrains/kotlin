/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

// TODO: Polyfill
internal fun imul(a_local: Int, b_local: Int): Int {
    val lhs = jsBitwiseAnd(a_local, js("0xffff0000")).toDouble() * jsBitwiseAnd(b_local, 0xffff).toDouble()
    val rhs = jsBitwiseAnd(a_local, 0xffff).toDouble() * b_local.toDouble()
    return jsBitwiseOr(lhs + rhs, 0)
}
