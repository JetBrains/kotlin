/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.lite

import org.jetbrains.kotlin.konan.KonanVersion
import org.junit.Test
import org.junit.Assert.*

class LiteKonanDistributionTests {

    @Test
    fun testDistribution() {
        val distribution = LiteKonanDistributionProvider.getDistribution(konanHomeDir)
            ?: error("Could not load Kotlin/Native distribution info from $konanHomeDir")

        assertEquals("Kotlin/Native distribution home", konanHomeDir, distribution.distributionHome)
        assertEquals("Kotlin/Native version string", expectedVersionString, distribution.konanVersionString)
        assertEquals("Kotlin/Native version", expectedVersion, distribution.konanVersion)
    }

    private companion object {
        const val expectedVersionString = "1.2.3-release-5678"

        val expectedVersion: KonanVersion
            get() = KonanVersion.fromString(expectedVersionString)
    }
}
