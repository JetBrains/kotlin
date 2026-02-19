/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*
import kotlin.native.internal.*
import kotlin.reflect.*

fun box(): String {
    val ktype = typeOf<Map<in String?, out List<*>>?>()
    assertTrue(ktype.isPermanent())
    assertEquals("Map", (ktype.classifier as? KClass<*>)?.simpleName)
    assertSame(Map::class, ktype.classifier)
    assertTrue(ktype.isMarkedNullable)
    assertTrue(ktype.arguments.isPermanent())
    assertEquals(2, ktype.arguments.size)
    assertSame(KVariance.IN, ktype.arguments[0].variance)
    assertSame(KVariance.OUT, ktype.arguments[1].variance)

    val arg0type = ktype.arguments[0].type!!
    assertTrue(arg0type.isPermanent())
    assertEquals("String", (arg0type.classifier as? KClass<*>)?.simpleName)
    assertSame(String::class, arg0type.classifier)
    assertTrue(arg0type.isMarkedNullable)
    assertTrue(arg0type.arguments.isPermanent())
    assertTrue(arg0type.arguments.isEmpty())

    val arg1type = ktype.arguments[1].type!!
    assertTrue(arg1type.isPermanent())
    assertEquals("List", (arg1type.classifier as? KClass<*>)?.simpleName)
    assertSame(List::class, arg1type.classifier)
    assertFalse(arg1type.isMarkedNullable)
    assertTrue(arg1type.arguments.isPermanent())
    assertTrue(arg1type.arguments.size == 1)
    assertSame(null, arg1type.arguments[0].variance)
    assertSame(null, arg1type.arguments[0].type)

    return "OK"
}
