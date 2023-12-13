/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.escapeAnalysis.string

import kotlin.test.*
import kotlin.native.internal.*

@Test fun runTest() {
    val s = String()
    assertTrue(s.isLocal())
}
