/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.ktype.ktype1

import kotlin.test.*
import kotlin.reflect.*

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified R> kType() = typeOf<R>()

inline fun <reified R> kType(obj: R) = kType<R>()

class C<T>
class D

fun <T> kTypeForCWithTypeParameter() = kType<C<T>>()

class Outer<T> {
    companion object Friend
    inner class Inner<S>
}

object Object

private val pkg = "codegen.ktype.ktype1"

@Test
fun testBasics1() {
    assertEquals("$pkg.C<kotlin.Int?>", kType<C<Int?>>().toString())
    assertEquals("$pkg.C<$pkg.C<kotlin.Any>>", kType<C<C<Any>>>().toString())

    assertEquals("$pkg.C<T>", kTypeForCWithTypeParameter<D>().toString())
    assertEquals("$pkg.Object", kType<Object>().toString())
    assertEquals("$pkg.Outer.Friend", kType<Outer.Friend>().toString())
}

@Test
fun testInner() {
    val innerKType = kType<Outer<D>.Inner<String>>()
    assertEquals(Outer.Inner::class, innerKType.classifier)
    assertEquals(String::class, innerKType.arguments.first().type!!.classifier)
    assertEquals(D::class, innerKType.arguments.last().type!!.classifier)
}

@Test
fun testAnonymousObject() {
    val obj = object {}
    val objType = kType(obj)

    assertEquals("(non-denotable type)", objType.toString())
    assertEquals(obj::class, objType.classifier)

    assertTrue(objType.arguments.isEmpty())
    assertFalse(objType.isMarkedNullable)
}