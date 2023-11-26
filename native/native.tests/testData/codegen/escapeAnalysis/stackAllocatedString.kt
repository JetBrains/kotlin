/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO

import kotlin.test.*
import kotlin.native.internal.*

fun box(): String {
    val s = String()
    assertTrue(s.isLocal())

    return "OK"
}
