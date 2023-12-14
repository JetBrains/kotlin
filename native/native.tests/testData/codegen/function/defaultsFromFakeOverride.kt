/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

interface I<T> {
    fun f(x: String = "OK"): String
}

open class A<T> {
    open fun f(x: String) = x
}

class B : A<String>(), I<String>

fun box(): String {
    val b = B()
    return b.f()
}