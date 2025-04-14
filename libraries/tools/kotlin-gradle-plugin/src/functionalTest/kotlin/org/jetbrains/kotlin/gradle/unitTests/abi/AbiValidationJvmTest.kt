/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.abi

import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.internal.abi.AbiValidationExtensionImpl
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class AbiValidationJvmTest {
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


    fun buildExtension(): AbiValidationExtensionImpl {
        val project = buildProjectWithJvm()

        val kotlin = project.kotlinJvmExtension
        project.evaluate()

        val abiValidation = kotlin.extensions.findByType<AbiValidationExtension>() as AbiValidationExtensionImpl
        assertNotNull(abiValidation, "ABI validation extension not found")
        return abiValidation
    }
}