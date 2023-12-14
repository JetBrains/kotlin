/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

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

fun box(): String {
    assertEquals("C<kotlin.Int?>", kType<C<Int?>>().toString())
    assertEquals("C<C<kotlin.Any>>", kType<C<C<Any>>>().toString())

    assertEquals("C<T>", kTypeForCWithTypeParameter<D>().toString())
    assertEquals("Object", kType<Object>().toString())
    assertEquals("Outer.Friend", kType<Outer.Friend>().toString())

    return "OK"
}
