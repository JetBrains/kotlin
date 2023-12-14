/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import kotlin.reflect.*

inline fun <reified T, R : T> reifiedUpperBound() = typeOf<List<R>>()

fun box(): String {
    val l = reifiedUpperBound<Any, Any>()
    assertEquals(List::class, l.classifier)
    val r = l.arguments.single().type!!.classifier
    assertTrue(r is KTypeParameter)
    assertFalse((r as KTypeParameter).isReified)
    assertEquals("R", (r as KTypeParameter).name)
    val t = (r as KTypeParameter).upperBounds.single().classifier
    assertTrue(t is KTypeParameter)
    assertTrue((t as KTypeParameter).isReified)
    assertEquals("T", (t as KTypeParameter).name)

    return "OK"
}
