/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.inline13

import kotlin.test.*

open class A<T1>()
class B<T2>() : A<T2>()

@Suppress("NOTHING_TO_INLINE")
inline fun <reified T: A<*>> foo(f: Any?): Boolean {
    return f is T?
}

fun bar(): Boolean {
    return foo<B<Int>>(B<Int>())
}

@Test fun runTest() {
    println(bar().toString())
}
