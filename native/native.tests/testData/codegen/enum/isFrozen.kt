/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)
@file:Suppress("DEPRECATION_ERROR")

import kotlin.test.*
import kotlin.native.concurrent.*

enum class Zzz(val zzz: String, var value: Int = 0) {
    Z1("z1"),
    Z2("z2")
}

fun box(): String {
    assertFalse(Zzz.Z1.isFrozen)
    Zzz.Z1.value = 42
    assertEquals(42, Zzz.Z1.value)

    return "OK"
}
