/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.abi

import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.abi.AbiValidationMultiplatformExtensionImpl
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class AbiValidationMultiplatformTest {
    @Test
    public fun testNames() {
        val extension = buildExtension()

        assertEquals(setOf("main"), extension.variants.getNames())
        extension.createVariant("extra")
        assertEquals(setOf("main", "extra"), extension.variants.getNames())
    }

    @Test
    public fun testConfigureWhenCreate() {
        val extension = buildExtension()

        extension.createVariant("extra") {
            it.filters.excluded.byNames.add("excluded")
        }

        assertTrue(extension.variants.named("main").get().filters.excluded.byNames.get().isEmpty())
        assertEquals(setOf("excluded"), extension.variants.named("extra").get().filters.excluded.byNames.get())
    }

    @Test
    public fun testConfigureByName() {
        val extension = buildExtension()

        extension.createVariant("extra")
        extension.configureVariant("extra") {
            it.filters.excluded.byNames.add("excluded")
        }

        assertTrue(extension.variants.named("main").get().filters.excluded.byNames.get().isEmpty())
        assertEquals(setOf("excluded"), extension.variants.named("extra").get().filters.excluded.byNames.get())
    }

    @Test
    public fun testConfigureEach() {
        val extension = buildExtension()

        extension.createVariant("extra")
        extension.configureAllVariants {
            it.filters.excluded.byNames.add("excluded")
        }

        assertEquals(setOf("excluded"), extension.variants.named("main").get().filters.excluded.byNames.get())
        assertEquals(setOf("excluded"), extension.variants.named("extra").get().filters.excluded.byNames.get())
    }


    fun buildExtension(): AbiValidationMultiplatformExtensionImpl {
        val project = buildProjectWithMPP()

        val kotlin = project.multiplatformExtension
        project.evaluate()

        val abiValidation = kotlin.extensions.findByType<AbiValidationMultiplatformExtension>() as AbiValidationMultiplatformExtensionImpl
        assertNotNull(abiValidation, "ABI validation extension not found")
        return abiValidation
    }
}