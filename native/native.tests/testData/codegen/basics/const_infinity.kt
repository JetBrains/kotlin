/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

//Original issue here https://youtrack.jetbrains.com/issue/KT-37212
@Suppress("DIVISION_BY_ZERO")
const val fpInfConst = 1.0F / 0.0F
@Suppress("DIVISION_BY_ZERO")
val fpInfVal = 1.0F / 0.0F

fun box(): String {
    assertEquals(fpInfConst, Float.POSITIVE_INFINITY)
    assertEquals(fpInfVal, Float.POSITIVE_INFINITY)
    assertEquals(fpInfConst, fpInfVal)

    return "OK"
}
