/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    // Simple loops
    for (i in 4..0) { return "FAIL 11 $i" }
    for (i in 4 until 0) { return "FAIL 12 $i" }
    for (i in 0 downTo 4) { return "FAIL 13 $i" }
    // Steps
    for (i in 4..0 step 2) { return "FAIL 21 $i" }
    for (i in 4 until 0 step 2) { return "FAIL 22 $i" }
    for (i in 0 downTo 4 step 2) { return "FAIL 23 $i" }
    // Two steps
    for (i in 6..0 step 2 step 3) { return "FAIL 31 $i" }
    for (i in 6 until 0 step 2 step 3) { return "FAIL 32 $i" }
    for (i in 0 downTo 6 step 2 step 3) { return "FAIL 33 $i" }

    return "OK"
}