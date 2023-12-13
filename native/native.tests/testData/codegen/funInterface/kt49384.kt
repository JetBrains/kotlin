/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.funInterface.kt49384

import kotlin.test.*

interface A<T>

// https://youtrack.jetbrains.com/issue/KT-49384
class B<T> {
    init {
        mutableListOf<A<out T>>()
                .sortWith { _, _ -> 1 }
    }
}

@Test
fun test1() {
    val b = B<Any>()
    assertEquals(b, b) // Just to ensure B is not deleted by DCE
}

fun interface Foo<T> {
    fun same(obj: T): T
}

fun getSame(obj: A<out Any>, foo: Foo<A<out Any>>) = foo.same(obj)

@Test
fun test2() {
    val obj = object : A<Any> {}
    assertSame(obj, getSame(obj) { it })
}
