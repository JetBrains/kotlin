/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.abi

import com.intellij.util.takeWhileInclusive
import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Test

class AbiVersionTest {
    @Test
    fun `Current ABI version less or equal to the latest stable LV`() {
        val (abiVersionMajor: Int, abiVersionMinor: Int, abiVersionPatch: Int) = KotlinAbiVersion.CURRENT

        val lvMajor: Int = LanguageVersion.LATEST_STABLE.major
        val lvMinor: Int = LanguageVersion.LATEST_STABLE.minor

        assertTrue(abiVersionMajor < lvMajor || (abiVersionMajor == lvMajor && abiVersionMinor <= lvMinor)) {
            "The current ABI version $abiVersionMajor.$abiVersionMinor.$abiVersionPatch should not be ahead of the latest stable LV ${LanguageVersion.LATEST_STABLE}"
        }
    }

    @Test
    fun `Each ABI version since 2_3 is reflected in KlibAbiCompatibilityLevel`() {
        fun KotlinAbiVersion.next() = KotlinAbiVersion(major, minor + 1, patch)

        val coveredAbiVersions: List<KotlinAbiVersion> = generateSequence(ABI_VERSION_2_3) { it.next() }
            .takeWhileInclusive { it != KotlinAbiVersion.CURRENT }
            .toList()

        val representedInAbiCompatibilityLevel: List<KotlinAbiVersion> =
            KlibAbiCompatibilityLevel.entries.map { it.toAbiVersionForManifest() }

        assertEquals(coveredAbiVersions, representedInAbiCompatibilityLevel)
    }

    @Test
    fun `The proper order of KlibAbiCompatibilityLevel`() {
        val actual = KlibAbiCompatibilityLevel.entries
        val expected = actual.sortedWith { left, right ->
            val majorDiff = left.major - right.major
            if (majorDiff != 0) majorDiff else left.minor - right.minor
        }

        assertEquals(expected, actual)
    }

    @Test
    fun `KlibAbiCompatibilityLevel_LATEST_STABLE is the last entry in enum`() {
        assertEquals(KlibAbiCompatibilityLevel.entries.last(), KlibAbiCompatibilityLevel.LATEST_STABLE)
    }

    companion object {
        private val ABI_VERSION_2_3 = KotlinAbiVersion(2, 3, 0)
    }
}
