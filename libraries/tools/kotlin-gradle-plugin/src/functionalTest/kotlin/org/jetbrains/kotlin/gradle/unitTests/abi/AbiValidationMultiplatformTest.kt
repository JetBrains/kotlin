/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.abi

import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec
import org.jetbrains.kotlin.gradle.dsl.abi.VariantConfigurator
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertNotNull

internal class AbiValidationMultiplatformTest : AbstractAbiValidationVariantsTest() {
    @Test
    public override fun testNames() {
        super.testNames()
    }

    @Test
    public override fun testNamed() {
        super.testNamed()
    }

    @Test
    public override fun testConfigureEach() {
        super.testConfigureEach()
    }

    @Test
    public override fun testMatching() {
        super.testMatching()
    }

    @Test
    public override fun testWithType() {
        super.testWithType()
    }

    override fun buildVariants(): VariantConfigurator<AbiValidationVariantSpec> {
        val project = buildProjectWithMPP()

        val kotlin = project.multiplatformExtension
        project.evaluate()

        val abiValidation = kotlin.extensions.findByType<AbiValidationMultiplatformExtension>()
        assertNotNull(abiValidation, "ABI validation extension not found")

        // common code
        @Suppress("UNCHECKED_CAST")
        return abiValidation.variants as VariantConfigurator<AbiValidationVariantSpec>
    }
}