/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.associatedObjects.associatedObjects1

import kotlin.test.*
import kotlin.reflect.*

@Test
@OptIn(ExperimentalAssociatedObjects::class)
fun testBasics1() {
    assertSame(Bar, Foo::class.findAssociatedObject<Associated1>())
    assertSame(Baz, Foo::class.findAssociatedObject<Associated2>())
    assertSame(null, Foo::class.findAssociatedObject<Associated3>())

    assertSame(null, Bar::class.findAssociatedObject<Associated1>())
}

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated1(val kClass: KClass<*>)

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated2(val kClass: KClass<*>)

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated3(val kClass: KClass<*>)

@Associated1(Bar::class)
@Associated2(Baz::class)
class Foo

object Bar
object Baz

@Test
@OptIn(ExperimentalAssociatedObjects::class)
fun testGlobalOptimizations1() {
    val i1 = I1ImplHolder::class.findAssociatedObject<Associated1>()!! as I1
    assertEquals(42, i1.foo())
    val c = C(null)
    i1.bar(c)
    assertEquals("zzz", c.list!![0])
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

@Test
@OptIn(ExperimentalAssociatedObjects::class)
fun testGlobalOptimizations2() {
    val i2 = I2ImplHolder()::class.findAssociatedObject<Associated1>()!! as I2
    assertEquals(17, i2.foo())
}

private interface I2 {
    fun foo(): Int
}

private object I2Impl : I2 {
    override fun foo() = 17
}

@Associated1(I2Impl::class)
private class I2ImplHolder