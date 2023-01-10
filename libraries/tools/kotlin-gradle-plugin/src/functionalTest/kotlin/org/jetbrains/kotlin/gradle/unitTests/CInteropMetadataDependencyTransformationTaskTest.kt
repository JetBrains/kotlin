/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.TaskInternal
import org.jetbrains.kotlin.gradle.util.MultiplatformExtensionTest
import org.jetbrains.kotlin.gradle.util.enableCInteropCommonization
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrRegisterCInteropMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrRegisterCInteropMetadataDependencyTransformationTaskForIde
import kotlin.test.*

class CInteropMetadataDependencyTransformationTaskTest : MultiplatformExtensionTest() {

    @Test
    fun `task not registered when cinterop commonization is disabled`() {
        project.enableCInteropCommonization(false)

        kotlin.linuxArm64()
        kotlin.linuxX64()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val linuxArm64Main = kotlin.sourceSets.getByName("linuxArm64Main")
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val linuxMain = kotlin.sourceSets.create("linuxMain") as DefaultKotlinSourceSet

        linuxMain.dependsOn(commonMain)
        linuxArm64Main.dependsOn(linuxMain)
        linuxX64Main.dependsOn(linuxMain)

        /* Expect no tasks being registered without the cinterop commonization feature flag */
        assertNull(project.locateOrRegisterCInteropMetadataDependencyTransformationTask(linuxMain))
        assertNull(project.locateOrRegisterCInteropMetadataDependencyTransformationTaskForIde(linuxMain))
    }

    @Test
    fun `test task ordering`() {
        project.enableCInteropCommonization(true)
        kotlin.linuxX64()
        kotlin.linuxArm64()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val linuxArm64Main = kotlin.sourceSets.getByName("linuxArm64Main")
        val linuxArm64Test = kotlin.sourceSets.getByName("linuxArm64Test")
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val linuxX64Test = kotlin.sourceSets.getByName("linuxX64Test")
        val nativeMain = kotlin.sourceSets.create("nativeMain") as DefaultKotlinSourceSet
        val nativeTest = kotlin.sourceSets.create("nativeTest") as DefaultKotlinSourceSet

        nativeMain.dependsOn(commonMain)
        linuxX64Main.dependsOn(nativeMain)
        linuxArm64Main.dependsOn(nativeMain)

        nativeTest.dependsOn(commonTest)
        linuxX64Test.dependsOn(nativeTest)
        linuxArm64Test.dependsOn(nativeTest)

        val nativeTestTransformationTask = project.locateOrRegisterCInteropMetadataDependencyTransformationTask(nativeTest)

        assertNotNull(nativeTestTransformationTask, "Expected transformation task registered for 'nativeTest'")
        assertEquals(
            listOf(commonMain, commonTest, nativeMain).map { sourceSet ->
                project.locateOrRegisterCInteropMetadataDependencyTransformationTask(sourceSet as DefaultKotlinSourceSet)?.get()
                    ?: fail("Expected transformation task registered for '${sourceSet.name}'")
            }.toSet(),
            nativeTestTransformationTask.get().mustRunAfter.getDependencies(null).toSet()
        )
    }

    @Test
    fun `test task disabled for non shared-native source sets`() {
        project.enableCInteropCommonization(true)
        kotlin.linuxArm64()
        kotlin.linuxX64()
        kotlin.jvm()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val linuxArm64Main = kotlin.sourceSets.getByName("linuxArm64Main")
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val linuxMain = kotlin.sourceSets.create("linuxMain") as DefaultKotlinSourceSet

        linuxMain.dependsOn(commonMain)
        linuxArm64Main.dependsOn(linuxMain)
        linuxX64Main.dependsOn(linuxMain)


        listOf(
            "commonMain", "jvmMain", "linuxArm64Main", "linuxX64Main"
        ).map { sourceSetName -> kotlin.sourceSets.getByName(sourceSetName) }.forEach { sourceSet ->
            val task = project.locateOrRegisterCInteropMetadataDependencyTransformationTask(sourceSet as DefaultKotlinSourceSet)
                ?: return@forEach

            assertFalse(
                task.get().onlyIf.isSatisfiedBy(task.get() as TaskInternal),
                "Expected task ${task.name} to be disabled (not a shared native source set)"
            )
        }

        val linuxMainTask = project.locateOrRegisterCInteropMetadataDependencyTransformationTaskForIde(linuxMain)
            ?: fail("Expected transformation task registered for 'linuxMain'")

        assertTrue(
            linuxMainTask.get().onlyIf.isSatisfiedBy(linuxMainTask.get() as TaskInternal),
            "Expected task ${linuxMainTask.name} to be enabled"
        )
    }
}
