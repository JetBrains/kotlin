/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.main
import org.jetbrains.kotlin.gradle.util.relativeTo
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinNativeCompileRefinesPathsTest {
    @Test
    fun `test - shared linux - refinesPaths`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.applyDefaultHierarchyTemplate()

        kotlin.linuxX64()
        kotlin.linuxArm64()

        project.evaluate()

        /* Check linuxMain metadata compilation */
        run {
            val compilation = kotlin.metadata().compilations.getByName("linuxMain") as KotlinSharedNativeCompilation
            val compileTask = compilation.compileTaskProvider.get()

            assertEquals(
                project.files(
                    "build/classes/kotlin/metadata/nativeMain/klib/test_nativeMain",
                    "build/classes/kotlin/metadata/commonMain/klib/test_commonMain"
                ).toSet().relativeTo(project),
                compileTask.refinesModule.files.relativeTo(project)
            )

            assertEquals(
                emptySet(), compileTask.friendModule.files.relativeTo(project),
                "Expected no friendModules on linuxMain metadata compilation"
            )
        }

        /* Check linuxX64Main platform compilation */
        run {
            val compilation = kotlin.linuxX64().compilations.main
            val compileTask = compilation.compileTaskProvider.get() as KotlinNativeCompile

            /* Platform compilation will compile all sources together, no refinesModules has to be set */
            assertEquals(
                emptySet(), compileTask.refinesModule.files.relativeTo(project)
            )
        }
    }
}