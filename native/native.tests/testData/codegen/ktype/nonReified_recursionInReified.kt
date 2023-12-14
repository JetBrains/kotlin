/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import kotlin.reflect.*

inline fun <reified T : Comparable<T>> recursionInReified() = typeOf<List<T>>()

fun box(): String {
    val l = recursionInReified<Int>()
    assertEquals(List::class, l.classifier)
    assertEquals(Int::class, l.arguments.single().type!!.classifier)

    return "OK"
}