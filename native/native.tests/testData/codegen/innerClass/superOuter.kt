/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

open class Outer(val outer: String) {
    open inner class Inner(val inner: String): Outer(inner) {
        fun foo() = outer
    }

    fun value() = Inner("OK").foo()
}

fun box() = Outer("Fail").value()
