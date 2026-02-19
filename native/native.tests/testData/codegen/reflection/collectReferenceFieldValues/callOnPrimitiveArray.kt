/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.native.internal.*
import kotlin.test.*

fun box(): String {
    assertEquals(intArrayOf(1, 2, 3).collectReferenceFieldValues(), emptyList<Any>())
    assertEquals(intArrayOf().collectReferenceFieldValues(), emptyList<Any>())

    return "OK"
}
