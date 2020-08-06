/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import kotlin.test.Test
import kotlin.test.assertTrue


class NpmRangeTest {
    @Test
    fun maxStartWith() {
        val nullRange1 = npmRange()
        val nullRange2 = npmRange()
        val maxStart1 = maxStart(
            nullRange1,
            nullRange2
        )
        assertTrue("Max start should be ${nullRange2.startVersion} but $maxStart1 found") {
            maxStart1 == nullRange2.startVersion
        }

        val startRange1 = npmRange(
            startMajor = 1
        )
        val maxStart2 = maxStart(
            startRange1,
            npmRange()
        )
        assertTrue("Max start should be ${startRange1.startVersion} but $maxStart2 found") {
            maxStart2 == startRange1.startVersion
        }

        val startRange2 = npmRange(startMajor = 2)
        val maxStart3 = maxStart(
            startRange1,
            startRange2
        )
        assertTrue("Max start should be ${startRange2.startVersion} but $maxStart3 found") {
            maxStart3 == startRange2.startVersion
        }
    }
}

private fun npmRange(
    startMajor: Int? = null,
    startMinor: Int? = null,
    startPatch: Int? = null,
    endMajor: Int? = null,
    endMinor: Int? = null,
    endPatch: Int? = null,
    startInclusive: Boolean = false,
    endInclusive: Boolean = false
): NpmRange =
    NpmRange(
        startVersion = semVer(startMajor, startMinor, startPatch),
        endVersion = semVer(endMajor, endMinor, endPatch),
        startInclusive = startInclusive,
        endInclusive = endInclusive
    )

private fun semVer(
    major: Int? = null,
    minor: Int? = null,
    patch: Int? = null
): SemVer? =
    if (major == null && minor == null && patch == null)
        null
    else {
        SemVer(
            (major ?: 0).toBigInteger(),
            (minor ?: 0).toBigInteger(),
            (patch ?: 0).toBigInteger()
        )
    }