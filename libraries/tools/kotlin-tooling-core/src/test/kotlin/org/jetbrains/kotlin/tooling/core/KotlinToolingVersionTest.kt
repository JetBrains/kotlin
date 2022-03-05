/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinToolingVersionTest {

    @Test
    fun compareMajorVersions() {
        assertTrue(
            KotlinToolingVersion("1.6.0") < KotlinToolingVersion("2.0.0")
        )

        assertTrue(
            KotlinToolingVersion("2.0.0") > KotlinToolingVersion("1.0.0")
        )

        assertEquals(0, KotlinToolingVersion("2.0.0").compareTo(KotlinToolingVersion("2.0.0")))
    }

    @Test
    fun compareMinorVersions() {
        assertTrue(
            KotlinToolingVersion("1.6.20") > KotlinToolingVersion("1.5.30")
        )

        assertTrue(
            KotlinToolingVersion("1.5.30") < KotlinToolingVersion("1.6.20")
        )

        assertTrue(
            KotlinToolingVersion("1.7.20-dev-100") > KotlinToolingVersion("1.6.0")
        )

        assertTrue(
            KotlinToolingVersion("1.7.20-dev-100") > KotlinToolingVersion("1.6")
        )
    }

    @Test
    fun comparePatchVersions() {
        assertTrue(
            KotlinToolingVersion("1.7.20") > KotlinToolingVersion("1.7.10")
        )

        assertTrue(
            KotlinToolingVersion("1.7.10") < KotlinToolingVersion("1.7.20")
        )

        assertTrue(
            KotlinToolingVersion("1.7.10-beta2-200") > KotlinToolingVersion("1.7.0")
        )
    }

    @Test
    fun compareMaturity() {
        assertTrue(
            KotlinToolingVersion("1.7.0") > KotlinToolingVersion("1.7.0-rc")
        )

        assertTrue(
            KotlinToolingVersion("1.7.0-rc") > KotlinToolingVersion("1.7.0-beta")
        )

        assertTrue(
            KotlinToolingVersion("1.7.0-beta") > KotlinToolingVersion("1.7.0-alpha")
        )

        assertTrue(
            KotlinToolingVersion("1.7.0-alpha") > KotlinToolingVersion("1.7.0-m1")
        )

        assertTrue(
            KotlinToolingVersion("1.7.0-m1") > KotlinToolingVersion("1.7.0-dev")
        )

        assertTrue(
            KotlinToolingVersion("1.7.0-dev") > KotlinToolingVersion("1.7.0-snapshot")
        )
    }

    @Test
    fun compareClassifierNumberAndBuildNumber() {
        assertTrue(
            KotlinToolingVersion("1.6.20-M1") < KotlinToolingVersion("1.6.20")
        )

        assertTrue(
            KotlinToolingVersion("1.6.20") > KotlinToolingVersion("1.6.20-1")
        )

        assertTrue(
            KotlinToolingVersion("1.6.20-1") < KotlinToolingVersion("1.6.20-2")
        )

        assertTrue(
            KotlinToolingVersion("1.6.20-M1") < KotlinToolingVersion("1.6.20-M2")
        )

        assertTrue(
            KotlinToolingVersion("1.6.20-M1-2") > KotlinToolingVersion("1.6.20-M1-1")
        )

        assertTrue(
            KotlinToolingVersion("1.6.20-M1-2") < KotlinToolingVersion("1.6.20-M2-1")
        )

        assertTrue(
            KotlinToolingVersion("1.6.20-M1-2") < KotlinToolingVersion("1.6.20-M2")
        )

        assertTrue(
            KotlinToolingVersion("1.6.20-beta1") > KotlinToolingVersion("1.6.20-beta")
        )

        assertTrue(
            KotlinToolingVersion("1.6.20-M1") > KotlinToolingVersion("1.6.20-M1-1")
        )
    }

    @Test
    fun maturityWithClassifierNumberAndBuildNumber() {
        assertEquals(
            KotlinToolingVersion.Maturity.STABLE,
            KotlinToolingVersion("1.6.20").maturity
        )

        assertEquals(
            KotlinToolingVersion.Maturity.STABLE,
            KotlinToolingVersion("1.6.20-999").maturity
        )

        assertEquals(
            KotlinToolingVersion.Maturity.STABLE,
            KotlinToolingVersion("1.6.20-release-999").maturity
        )

        assertEquals(
            KotlinToolingVersion.Maturity.STABLE,
            KotlinToolingVersion("1.6.20-rElEaSe-999").maturity
        )

        assertEquals(
            KotlinToolingVersion.Maturity.RC,
            KotlinToolingVersion("1.6.20-rc2411-1901").maturity
        )

        assertEquals(
            KotlinToolingVersion.Maturity.RC,
            KotlinToolingVersion("1.6.20-RC2411-1901").maturity
        )

        assertEquals(
            KotlinToolingVersion.Maturity.BETA,
            KotlinToolingVersion("1.6.20-beta2411-1901").maturity
        )

        assertEquals(
            KotlinToolingVersion.Maturity.BETA,
            KotlinToolingVersion("1.6.20-bEtA2411-1901").maturity
        )

        assertEquals(
            KotlinToolingVersion.Maturity.ALPHA,
            KotlinToolingVersion("1.6.20-alpha2411-1901").maturity
        )

        assertEquals(
            KotlinToolingVersion.Maturity.ALPHA,
            KotlinToolingVersion("1.6.20-aLpHa2411-1901").maturity
        )

        assertEquals(
            KotlinToolingVersion.Maturity.MILESTONE,
            KotlinToolingVersion("1.6.20-m2411-1901").maturity
        )

        assertEquals(
            KotlinToolingVersion.Maturity.MILESTONE,
            KotlinToolingVersion("1.6.20-M2411-1901").maturity
        )
    }

    @Test
    fun invalidMilestoneVersion() {
        val exception = assertFailsWith<IllegalArgumentException> { KotlinToolingVersion("1.6.20-M") }
        assertTrue("maturity" in exception.message.orEmpty().toLowerCase(), "Expected 'maturity' issue mentioned in error message")
    }

    @Test
    fun buildNumber() {
        assertEquals(510, KotlinToolingVersion("1.6.20-510").buildNumber)
        assertEquals(510, KotlinToolingVersion("1.6.20-release-510").buildNumber)
        assertEquals(510, KotlinToolingVersion("1.6.20-rc1-510").buildNumber)
        assertEquals(510, KotlinToolingVersion("1.6.20-beta1-510").buildNumber)
        assertEquals(510, KotlinToolingVersion("1.6.20-alpha1-510").buildNumber)
        assertEquals(510, KotlinToolingVersion("1.6.20-m1-510").buildNumber)
    }

    @Test
    fun classifierNumber() {
        assertEquals(2, KotlinToolingVersion("1.6.20-rc2-510").classifierNumber)
        assertEquals(2, KotlinToolingVersion("1.6.20-beta2-510").classifierNumber)
        assertEquals(2, KotlinToolingVersion("1.6.20-alpha2-510").classifierNumber)
        assertEquals(2, KotlinToolingVersion("1.6.20-m2-510").classifierNumber)

        assertEquals(2, KotlinToolingVersion("1.6.20-rc2").classifierNumber)
        assertEquals(2, KotlinToolingVersion("1.6.20-beta2").classifierNumber)
        assertEquals(2, KotlinToolingVersion("1.6.20-alpha2").classifierNumber)
        assertEquals(2, KotlinToolingVersion("1.6.20-m2").classifierNumber)
    }

    @Test
    fun toKotlinVersion() {
        assertEquals(KotlinVersion(1, 7, 20), KotlinToolingVersion("1.7.20-rc2-202").toKotlinVersion())
    }

    @Test
    fun toKotlinToolingVersion() {
        assertEquals(KotlinToolingVersion("1.7"), KotlinVersion(1, 7).toKotlinToolingVersion())
    }

    @Test
    fun isMaturity() {
        assertTrue(
            KotlinToolingVersion("1.7").isStable
        )

        assertFalse(
            KotlinToolingVersion("1.7").isPreRelease
        )

        assertTrue(
            KotlinToolingVersion("1.7.0-rc").isRC
        )

        assertTrue(
            KotlinToolingVersion("1.7.0-beta").isBeta
        )

        assertTrue(
            KotlinToolingVersion("1.7.0-alpha").isAlpha
        )

        assertTrue(
            KotlinToolingVersion("1.7.0-m1").isMilestone
        )

        assertTrue(
            KotlinToolingVersion("1.7.0-dev").isDev
        )

        assertTrue(
            KotlinToolingVersion("1.7.0-snapshot").isSnapshot
        )
    }
}
