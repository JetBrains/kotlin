/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DEPRECATION")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Test will check that the deprecated 'KotlinTargetHierarchyDsl' will act exactly the same as the new
 * 'KotlinHierarchyDsl'
 */
class DeprecatedKotlinTargetHierarchyDslTest {

    val legacyProject = buildProjectWithMPP()
    val project = buildProjectWithMPP()

    @Test
    fun `test - default`() {
        legacyProject.multiplatformExtension.targetHierarchy.default()
        project.multiplatformExtension.applyDefaultHierarchyTemplate()

        assertEquals(
            project.multiplatformExtension.hierarchy.appliedTemplates,
            legacyProject.multiplatformExtension.hierarchy.appliedTemplates
        )
    }

    @Test
    fun `test - default with extension`() {
        val extension: KotlinHierarchyBuilder.Root.() -> Unit = {
            common {
                group("nix") {
                    withLinux()
                    withMacos()
                }
            }
        }
        legacyProject.multiplatformExtension.targetHierarchy.default(extension)
        project.multiplatformExtension.applyDefaultHierarchyTemplate(extension)

        listOf(legacyProject, project).forEach { project ->
            project.multiplatformExtension.jvm()
            project.multiplatformExtension.linuxX64()
            project.multiplatformExtension.macosX64()
            project.evaluate()

            if ("nativeMain" !in project.multiplatformExtension.sourceSets.names)
                fail("Expected 'nativeMain' in source sets")

            if ("nixMain" !in project.multiplatformExtension.sourceSets.names) {
                fail("Expected 'nixMain' in source sets")
            }
        }
    }

    @Test
    fun `test - custom`() {
        val description: KotlinHierarchyBuilder.Root.() -> Unit = { /* Does not matter */ }
        legacyProject.multiplatformExtension.targetHierarchy.custom(description)
        project.multiplatformExtension.applyHierarchyTemplate(description)

        assertEquals(
            project.multiplatformExtension.hierarchy.appliedTemplates,
            legacyProject.multiplatformExtension.hierarchy.appliedTemplates
        )
    }
}
