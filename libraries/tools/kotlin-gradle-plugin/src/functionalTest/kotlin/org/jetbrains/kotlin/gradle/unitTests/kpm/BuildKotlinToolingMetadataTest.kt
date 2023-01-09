/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.kpm

import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.native
import org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.tooling.buildKotlinToolingMetadataTask
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import kotlin.test.Test
import kotlin.test.assertEquals

private fun Project.kotlinToolingMetadataOfModule(moduleName: String): KotlinToolingMetadata {
    val module = pm20Extension.modules.getByName(moduleName)
    return module.buildKotlinToolingMetadataTask!!.get().kotlinToolingMetadata
}

private val Project.kotlinToolingMetadataOfMainModule get() = kotlinToolingMetadataOfModule(GradleKpmModule.MAIN_MODULE_NAME)

class BuildKotlinToolingMetadataTest : AbstractKpmExtensionTest() {
    @Test
    fun `multiple targets`() {
        // Given
        with(kotlin) {
            mainAndTest {
                jvm
                val linux = fragments.create("linux")
                fragments.create<GradleKpmLinuxX64Variant>("linuxX64").apply { refines(linux) }
                fragments.create<GradleKpmLinuxArm64Variant>("linuxArm64").apply { refines(linux) }
                // No JS & Android variants available at the moment, only through [LegacyMappedVariant] which is tested below
            }
        }

        // When
        val metadata = project.kotlinToolingMetadataOfMainModule

        // Then
        assertEquals("Gradle", metadata.buildSystem)
        assertEquals(project.gradle.gradleVersion, metadata.buildSystemVersion)
        assertEquals(KotlinPm20PluginWrapper::class.java.canonicalName, metadata.buildPlugin)
        assertEquals(project.getKotlinPluginVersion(), metadata.buildPluginVersion)
        assertEquals(3, metadata.projectTargets.size, "Expected 3 targets in KPM")

        val jvmTarget = metadata.projectTargets.single { it.platformType == jvm.name }
        assertEquals(GradleKpmJvmVariant::class.decoratedClassCanonicalName, jvmTarget.target)

        val nativeTargets = metadata.projectTargets.filter { it.platformType == native.name }.map { it.target }.toSet()
        assertEquals(
            setOf(
                GradleKpmLinuxArm64Variant::class.decoratedClassCanonicalName,
                GradleKpmLinuxX64Variant::class.decoratedClassCanonicalName,
            ),
            nativeTargets
        )
    }
}
