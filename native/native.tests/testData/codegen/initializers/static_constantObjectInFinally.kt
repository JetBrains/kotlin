/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*
import kotlin.native.internal.*
import kotlin.reflect.*

inline fun invokeAndReturnKClass(block: ()->Boolean) : KClass<*> {
    try {
        if (block()) {
            return Double::class
        }
    } catch (e: Exception) {
        return String::class
    } finally {
        return Int::class
    }
}

fun box(): String {
    for (i in 0..2) {
        val clazz = invokeAndReturnKClass {
            when (i) {
                0 -> true
                1 -> false
                else -> TODO("test")
            }
        }
        assertTrue(clazz.isPermanent())
        assertEquals("kotlin.Int", clazz.qualifiedName)
    }

    return "OK"
}
