/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class KotlinNativeVariantFactoryTest(
    private val variantConstructor: KotlinNativeVariantConstructor<*>,
    @Suppress("unused") private val variantClassName: String
) : AbstractKpmExtensionTest() {

    private lateinit var variant: KotlinNativeVariantInternal

    @BeforeTest
    fun createVariant() {
        variant = KotlinNativeVariantFactory(kotlin.main, variantConstructor).create("native")
    }

    @Test
    fun `test platform type`() {
        assertEquals(KotlinPlatformType.native, variant.platformType)
    }

    @Test
    fun `test konanTarget`() {
        assertEquals(variantConstructor.konanTarget, variant.konanTarget)
    }

    @Test
    fun `test variantClass`() {
        assertEquals(variantConstructor.variantClass, variant.javaClass)
    }

    @Test
    fun `test compileDependenciesConfiguration - contains konanTarget`() {
        assertEquals(
            variantConstructor.konanTarget.name,
            variant.compileDependenciesConfiguration
                .attributes.getAttribute(KotlinNativeTarget.konanTargetAttribute)
        )
    }

    @Test
    fun `test apiElementsConfiguration - contains konanTarget`() {
        assertEquals(
            variantConstructor.konanTarget.name,
            variant.apiElementsConfiguration
                .attributes.getAttribute(KotlinNativeTarget.konanTargetAttribute)
        )
    }

    @Test
    fun `test has source roots`() {
        assertTrue(variant.kotlinSourceRoots.srcDirs.isNotEmpty())
    }

    @Test
    fun `test sourceArchiveTaskName registered`() {
        assertTrue(project.tasks.getByName(variant.sourceArchiveTaskName) is Zip)
    }

    @Test
    fun `test compileTask registered`() {
        assertTrue(
            project.tasks.getByName(variant.compilationData.compileKotlinTaskName) is KotlinNativeCompile,
            "Expected registered compileKotlinTask to be instance of ${KotlinNativeCompile::class.simpleName}"
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun data() = listOf(
            KotlinLinuxX64Variant.constructor,
            KotlinMacosX64Variant.constructor,
            KotlinMacosX64Variant.constructor,
            KotlinMacosArm64Variant.constructor,
            KotlinIosX64Variant.constructor,
            KotlinIosArm64Variant.constructor
        ).map { arrayOf(it, it.variantClass.simpleName) }
    }
}
