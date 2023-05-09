/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.internal.TaskInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrRegisterCInteropMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrRegisterCInteropMetadataDependencyTransformationTaskForIde
import org.jetbrains.kotlin.gradle.util.MultiplatformExtensionTest
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableCInteropCommonization
import org.jetbrains.kotlin.gradle.util.kotlin
import java.io.File
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

        project.evaluate()
        val nativeTestTransformationTask = project.locateOrRegisterCInteropMetadataDependencyTransformationTask(nativeTest)

        assertNotNull(nativeTestTransformationTask, "Expected transformation task registered for 'nativeTest'")
        assertEquals(
            listOf(commonMain, commonTest, nativeMain).flatMap { sourceSet ->
                listOf(
                    project.locateOrRegisterCInteropMetadataDependencyTransformationTask(sourceSet as DefaultKotlinSourceSet)?.get()
                        ?: fail("Expected transformation task registered for '${sourceSet.name}'"),
                    project.locateOrRegisterCInteropMetadataDependencyTransformationTaskForIde(sourceSet)?.get()
                        ?: fail("Expected transformation task registered for '${sourceSet.name}'(forIde)")
                )
            }.toSet(),
            nativeTestTransformationTask.get().mustRunAfter.getDependencies(null).toSet()
        )
    }

    @Test
    fun `test task disabled for non shared-native source sets`() {
        project.enableCInteropCommonization(true)
        kotlin.targetHierarchy.default()
        kotlin.linuxArm64()
        kotlin.linuxX64()
        kotlin.jvm()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val linuxArm64Main = kotlin.sourceSets.getByName("linuxArm64Main")
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain") as DefaultKotlinSourceSet

        linuxMain.dependsOn(commonMain)
        linuxArm64Main.dependsOn(linuxMain)
        linuxX64Main.dependsOn(linuxMain)

        project.evaluate()

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

    @Test
    fun `test IDE task outputs doesnt conflict`() {
        fun projectWithCinterops(name: String, parent: Project? = null) = buildProjectWithMPP(
            projectBuilder = { withName(name); if (parent != null) withParent(parent) }
        ) {
            enableCInteropCommonization(true)
            kotlin {
                targetHierarchy.default()
                linuxX64()
                linuxArm64()
            }
        }.also { it.evaluate() }

        fun Project.transformationTaskOutputs(): Set<File> {
            val kotlin = multiplatformExtension
            val nativeMain = kotlin.sourceSets.findByName("nativeMain") ?: fail("Expected source set 'nativeMain")
            val cinteropTransformationTaskProvider = locateOrRegisterCInteropMetadataDependencyTransformationTaskForIde(
                nativeMain as DefaultKotlinSourceSet
            ) ?: fail("Expected transformation task registered for '$nativeMain'")
            val cinteropTransformationTask = cinteropTransformationTaskProvider.get()
            val projectRootDir = rootDir
            return cinteropTransformationTask
                .outputs
                .files
                .map { it.relativeTo(projectRootDir) }
                .toSet()
                // common root directory where all transformations stored
                .minus(File(".gradle/kotlin/kotlinTransformedCInteropMetadataLibraries"))
        }

        fun assertTasksOutputsDoesntIntersect(a: Project, b: Project) {
            val outputsA = a.transformationTaskOutputs()
            val outputsB = b.transformationTaskOutputs()

            val intersection = outputsA intersect outputsB

            assertTrue(
                actual = intersection.isEmpty(),
                message = """CInteropMetadataDependencyTransformationTaskForIde outputs conflicts for projects $a and $b :
                    |Same output files: ${intersection.toList()}
                """.trimMargin()
            )
        }

        val rootProject = ProjectBuilder.builder().build()
        val projectFoo = projectWithCinterops("foo", rootProject)
        val projectFooBar = projectWithCinterops("bar", projectFoo)
        val projectFooFoo = projectWithCinterops("foo", projectFoo)
        val projectBar = projectWithCinterops("bar", rootProject)

        assertTasksOutputsDoesntIntersect(projectFoo, projectBar)
        assertTasksOutputsDoesntIntersect(projectFoo, projectFooBar)
        assertTasksOutputsDoesntIntersect(projectFoo, projectFooFoo)

        // KT-56087
        // val projectFooDotBar = projectWithCinterops("foo.bar", rootProject)
        // assertTasksOutputsDoesntIntersect(projectFooBar, projectFooDotBar)
    }
}
