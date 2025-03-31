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
import kotlin.test.assertNotNull

internal class AbiValidationJvmTest : AbstractAbiValidationVariantsTest() {

    override fun buildVariants(): VariantConfigurator<AbiValidationVariantSpec> {
        val project = buildProjectWithJvm()

        val kotlin = project.kotlinJvmExtension
        project.evaluate()

        val abiValidation = kotlin.extensions.findByType<AbiValidationExtension>()
        assertNotNull(abiValidation, "ABI validation extension not found")
        return abiValidation.variants
    }
}