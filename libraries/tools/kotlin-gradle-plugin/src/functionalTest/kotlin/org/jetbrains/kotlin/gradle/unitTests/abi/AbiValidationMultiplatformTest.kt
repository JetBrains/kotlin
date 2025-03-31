/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.abi

import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec
import org.jetbrains.kotlin.gradle.dsl.abi.VariantConfigurator
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.assertNotNull

internal class AbiValidationMultiplatformTest : AbstractAbiValidationVariantsTest() {
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