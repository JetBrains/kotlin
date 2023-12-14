/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import kotlin.reflect.*

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified R> kType() = typeOf<R>()

class D
class Outer<T> {
    companion object Friend
    inner class Inner<S>
}

fun box(): String {
    val innerKType = kType<Outer<D>.Inner<String>>()
    assertEquals(Outer.Inner::class, innerKType.classifier)
    assertEquals(String::class, innerKType.arguments.first().type!!.classifier)
    assertEquals(D::class, innerKType.arguments.last().type!!.classifier)

    return "OK"
}
