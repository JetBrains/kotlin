/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.native.internal.*
import kotlin.test.*

fun box(): String {
    assertEquals(arrayOf(1, 2, 3).collectReferenceFieldValues(), listOf<Any>(1, 2, 3))
    assertEquals(arrayOf(null, "10", null, 3).collectReferenceFieldValues(), listOf<Any>("10", 3))
    assertEquals(arrayOf<Any>().collectReferenceFieldValues(), emptyList<Any>())
    assertEquals(emptyArray<Any>().collectReferenceFieldValues(), emptyList<Any>())
    assertEquals(emptyArray<Any?>().collectReferenceFieldValues(), emptyList<Any>())

    return "OK"
}
