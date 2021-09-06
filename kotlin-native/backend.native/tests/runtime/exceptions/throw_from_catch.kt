/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.exceptions.throw_from_catch

import kotlin.native.internal.*
import kotlin.test.*

@Test
fun runTest() {
    assertFailsWith<IllegalStateException>("My another error") {
        val frame = runtimeGetCurrentFrame()
        try {
            error("My error")
        } catch (e: Throwable) {
            assertEquals(frame, runtimeGetCurrentFrame())
            error("My another error")
        }
    }
}
