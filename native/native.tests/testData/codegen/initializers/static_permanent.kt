/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*
import kotlin.native.internal.*
import kotlin.reflect.*

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) : String {
        assertTrue(property.isPermanent());
        assertTrue(property.returnType.isPermanent())
        return property.name
    }
}

class A {
    val z by Delegate()
}

fun f() = 5

fun box(): String {
    val x = typeOf<Map<String, Int>>()
    assertTrue(x.isPermanent())
    val a = A()
    assertTrue(a.z.isPermanent())
    assertEquals("z", a.z)
    val t = ::f
    assertTrue(t.isPermanent())
    assertEquals(5, t())
    val z = { 6 }
    assertTrue(z.isPermanent())
    assertEquals(6, z())

    return "OK"
}
