/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SizeTest {
    @Test
    fun `size units contain the expected number of bytes`() {
        assertEquals(1L, 1.bytes.inWholeBytes)
        assertEquals(1_000L, 1.kilobytes.inWholeBytes)
        assertEquals(1_024L, 1.KiB.inWholeBytes)
        assertEquals(1_000_000L, 1.megabytes.inWholeBytes)
        assertEquals(1_048_576L, 1.MiB.inWholeBytes)
        assertEquals(1_000_000_000L, 1.gigabytes.inWholeBytes)
        assertEquals(1_073_741_824L, 1.GiB.inWholeBytes)
    }

    @Test
    fun `whole units discard fractional parts`() {
        val size = Size(1_234_567_890L)

        assertEquals(1_234_567L, size.inWholeKilobytes)
        assertEquals(1_205_632L, size.inWholeKiB)
        assertEquals(1_234L, size.inWholeMegabytes)
        assertEquals(1_177L, size.inWholeMiB)
        assertEquals(1L, size.inWholeGigabytes)
        assertEquals(1L, size.inWholeGiB)
    }

    @Test
    fun `sizes support arithmetic`() {
        assertEquals(6_000L, (2.kilobytes * 3).inWholeBytes)
        assertEquals(6_000L, (2.kilobytes * 3L).inWholeBytes)
        assertEquals(3_024L, (2.kilobytes + 1.KiB).inWholeBytes)
        assertEquals(976L, (2.kilobytes - 1.KiB).inWholeBytes)
    }
}