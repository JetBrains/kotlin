/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import kotlin.reflect.*

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified R> kType() = typeOf<R>()

inline fun <reified R> kType(obj: R) = kType<R>()

fun box(): String {
    val obj = object {}
    val objType = kType(obj)

    assertEquals("(non-denotable type)", objType.toString())
    assertEquals(obj::class, objType.classifier)

    assertTrue(objType.arguments.isEmpty())
    assertFalse(objType.isMarkedNullable)

    return "OK"
}