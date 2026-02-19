/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import test.io.serializeAndDeserialize
import kotlin.test.*
import kotlin.time.Instant

class InstantJVMTest {
    @Test
    fun serialize() {
        listOf(
            Instant.MIN,
            Instant.MAX,
            Instant.DISTANT_PAST,
            Instant.DISTANT_FUTURE,
            Instant.fromEpochSeconds(0),
            Instant.fromEpochSeconds(1, 1),
            Instant.fromEpochSeconds(-1, 1),
        ).forEach {
            assertEquals(it, serializeAndDeserialize(it))
        }
    }
}