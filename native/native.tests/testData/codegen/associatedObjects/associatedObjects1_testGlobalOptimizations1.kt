/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated1(val kClass: KClass<*>)

@OptIn(ExperimentalAssociatedObjects::class)
fun box(): String {
    val i1 = I1ImplHolder::class.findAssociatedObject<Associated1>()!! as I1
    assertEquals(42, i1.foo())
    val c = C(null)
    i1.bar(c)
    assertEquals("zzz", c.list!![0])

    return "OK"
}

private class C(var list: List<String>?)

private interface I1 {
    fun foo(): Int
    fun bar(c: C)
}

private object I1Impl : I1 {
    override fun foo() = 42
    override fun bar(c: C) {
        c.list = mutableListOf("zzz")
    }
}

@Associated1(I1Impl::class)
private class I1ImplHolder
