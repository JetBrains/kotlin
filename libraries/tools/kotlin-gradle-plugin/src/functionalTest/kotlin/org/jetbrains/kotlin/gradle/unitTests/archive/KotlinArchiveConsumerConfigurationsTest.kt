/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.archive

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.KarLayout
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.targets.native.resolvableApiConfiguration
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Configurations derived from a native compilation classpath must resolve Kotlin Archive dependencies to the same
 * platform klibs as the classpath itself, or Swift Export sees the raw archive.
 */
class KotlinArchiveConsumerConfigurationsTest {

    @Test
    fun `compile classpath requests Kotlin Archive platform artifacts`() {
        val project = buildProjectWithMPP { kotlin { iosArm64() } }.evaluate()

        assertEquals(
            KarLayout.Attributes.State.PLATFORM_ARTIFACTS_EXTRACTED,
            project.iosArm64MainCompilation.internal.configurations.compileDependencyConfiguration.karState,
        )
    }

    @Test
    fun `native api configuration requests Kotlin Archive platform artifacts`() {
        val project = buildProjectWithMPP { kotlin { iosArm64() } }.evaluate()

        assertEquals(
            KarLayout.Attributes.State.PLATFORM_ARTIFACTS_EXTRACTED,
            project.iosArm64MainCompilation.resolvableApiConfiguration().karState,
        )
    }

    private val org.gradle.api.Project.iosArm64MainCompilation
        get() = (multiplatformExtension.targets.getByName("iosArm64") as KotlinNativeTarget)
            .compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    private val Configuration.karState: String?
        get() = attributes.getAttribute(KarLayout.Attributes.state)
}
