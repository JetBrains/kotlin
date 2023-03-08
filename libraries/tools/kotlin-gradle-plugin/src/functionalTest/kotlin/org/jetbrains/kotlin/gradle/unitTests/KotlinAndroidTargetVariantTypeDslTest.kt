/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import junit.framework.TestCase.assertNull
import org.jetbrains.kotlin.gradle.plugin.awaitFinalValue
import org.jetbrains.kotlin.gradle.plugin.kotlinMultiplatformPluginLifecycle
import org.jetbrains.kotlin.gradle.targets.android.InternalKotlinAndroidTargetVariantTypeDsl
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.junit.Test

class KotlinAndroidTargetVariantTypeDslTest {
    private val project = buildProjectWithMPP()


    @Test
    fun `test -  targetHierarchyModuleName - not set`() {
        val dsl = InternalKotlinAndroidTargetVariantTypeDsl(project)
        project.kotlinMultiplatformPluginLifecycle.launch {
            assertNull(dsl.kotlinModuleName.orNull)
            assertNull(dsl.kotlinModuleName.awaitFinalValue())
        }

        project.evaluate()
    }
}