/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

interface A<T>

// https://youtrack.jetbrains.com/issue/KT-49384
fun interface Foo<T> {
    fun same(obj: T): T
}

fun getSame(obj: A<out Any>, foo: Foo<A<out Any>>) = foo.same(obj)

fun box(): String {
    val obj = object : A<Any> {}
    assertSame(obj, getSame(obj) { it })

    return "OK"
}
