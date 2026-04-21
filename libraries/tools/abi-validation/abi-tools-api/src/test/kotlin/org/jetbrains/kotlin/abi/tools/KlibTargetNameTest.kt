/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class KlibTargetNameTest {
    @Test
    fun parse() {
        assertEquals("a.b", KlibTarget("a", "b").toString())
        assertEquals("a", KlibTarget("a").toString())
        assertEquals("a", KlibTarget("a", "a").toString())

        assertThrows<IllegalArgumentException> { KlibTarget.parse("") }
        assertThrows<IllegalArgumentException> { KlibTarget.parse(" ") }
        assertThrows<IllegalArgumentException> { KlibTarget.parse("a.b.c") }
        assertThrows<IllegalArgumentException> { KlibTarget.parse("a.") }
        assertThrows<IllegalArgumentException> { KlibTarget.parse(".a") }

        KlibTarget.parse("a.b").also {
            assertEquals("b", it.configurableName)
            assertEquals("a", it.targetName)
        }

        KlibTarget.parse("a.a").also {
            assertEquals("a", it.configurableName)
            assertEquals("a", it.targetName)
        }

        KlibTarget.parse("a").also {
            assertEquals("a", it.configurableName)
            assertEquals("a", it.targetName)
        }
    }

    @Test
    fun validate() {
        assertThrows<IllegalArgumentException> {
            KlibTarget("a.b", "c")
        }
        assertThrows<IllegalArgumentException> {
            KlibTarget("a", "b.c")
        }
    }

    @Test
    fun targetsEqual() {
        assertEquals(KlibTarget.parse("androidNativeArm64"), KlibTarget.parse("androidNativeArm64"))
        assertNotEquals(KlibTarget.parse("androidNativeArm64"), KlibTarget.parse("androidNativeArm32"))

        assertEquals(
            KlibTarget.parse("androidNativeArm64.android"), KlibTarget.parse("androidNativeArm64.android")
        )
        assertNotEquals(
            KlibTarget.parse("androidNativeArm64.android"), KlibTarget.parse("androidNativeArm64")
        )

        assertEquals(
            KlibTarget.parse("androidNativeArm64.androidNativeArm64"),
            KlibTarget.parse("androidNativeArm64")
        )
    }

    @Test
    fun targetHashCode() {
        assertEquals(
            KlibTarget.parse("androidNativeArm64").hashCode(),
            KlibTarget.parse("androidNativeArm64").hashCode()
        )
        assertNotEquals(
            KlibTarget.parse("androidNativeArm64").hashCode(),
            KlibTarget.parse("androidNativeArm32").hashCode()
        )

        assertEquals(
            KlibTarget.parse("androidNativeArm64.android").hashCode(),
            KlibTarget.parse("androidNativeArm64.android").hashCode()
        )
        assertNotEquals(
            KlibTarget.parse("androidNativeArm64.android").hashCode(),
            KlibTarget.parse("androidNativeArm64").hashCode()
        )

        assertEquals(
            KlibTarget.parse("androidNativeArm64.androidNativeArm64").hashCode(),
            KlibTarget.parse("androidNativeArm64").hashCode()
        )
    }
}
