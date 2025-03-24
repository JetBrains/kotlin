/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.abi

import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec
import org.jetbrains.kotlin.gradle.dsl.abi.VariantConfigurator
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import kotlin.test.Test
import kotlin.test.assertNotNull

internal class AbiValidationJvmTest : AbstractAbiValidationVariantsTest() {
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
        val project = buildProjectWithJvm()

        val kotlin = project.kotlinJvmExtension
        project.evaluate()

        val abiValidation = kotlin.extensions.findByType<AbiValidationExtension>()
        assertNotNull(abiValidation, "ABI validation extension not found")
        return abiValidation.variants
    }
}