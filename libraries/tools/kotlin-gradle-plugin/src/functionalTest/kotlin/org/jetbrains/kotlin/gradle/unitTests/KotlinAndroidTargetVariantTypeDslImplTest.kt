/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import junit.framework.TestCase.assertNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.awaitFinalValue
import org.jetbrains.kotlin.gradle.plugin.currentKotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.targets.android.KotlinAndroidTargetVariantTypeDslImpl
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class KotlinAndroidTargetVariantTypeDslImplTest {
    private val project = buildProjectWithMPP()
    private val dsl = KotlinAndroidTargetVariantTypeDslImpl(project)


    @Test
    fun `test -  module - not set`() = project.runLifecycleAwareTest {
        project.kotlinPluginLifecycle.launch {
            assertNull(dsl.targetHierarchy.module.orNull)
            assertNull(dsl.targetHierarchy.module.awaitFinalValue())
        }
    }

    @Test
    fun `test - module - can be set in users afterEvaluate`() = project.runLifecycleAwareTest {
        afterEvaluate { dsl.targetHierarchy.module.set(KotlinTargetHierarchy.ModuleName("x")) }
        dsl.targetHierarchy.module.set(KotlinTargetHierarchy.ModuleName("-set-before-after-evaluate-"))
        assertEquals("x", dsl.targetHierarchy.module.awaitFinalValue()?.name)
        assertEquals(KotlinPluginLifecycle.Stage.FinaliseDsl, currentKotlinPluginLifecycle().stage)
    }

    @Test
    fun `test - module - cannot be set after FinaliseDsl`() = project.runLifecycleAwareTest {
        launchInStage(KotlinPluginLifecycle.Stage.BeforeFinaliseDsl) {
            dsl.targetHierarchy.module.set(KotlinTargetHierarchy.ModuleName("x"))
        }

        launchInStage(KotlinPluginLifecycle.Stage.FinaliseDsl) {
            assertFails { dsl.targetHierarchy.module.set(KotlinTargetHierarchy.ModuleName("y")) }
        }
    }
}