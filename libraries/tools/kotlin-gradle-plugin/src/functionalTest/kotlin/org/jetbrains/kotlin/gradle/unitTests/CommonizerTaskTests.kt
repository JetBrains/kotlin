/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.targets.native.internal.cInteropCommonizationEnabled
import org.jetbrains.kotlin.gradle.targets.native.internal.copyCommonizeCInteropForIdeTask
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.*

class CommonizerTaskTests {

    companion object {
        const val JVM_ECOSYSTEM_PLUGIN_ID = "jvm-ecosystem"
    }

    private val rootProject = ProjectBuilder.builder().build() as ProjectInternal
    private val subproject = ProjectBuilder.builder().withName("subproject").withParent(rootProject).build() as ProjectInternal

    private fun configureDefaultTestProjects() {

        subproject.applyMultiplatformPlugin().apply {
            linuxX64()
            linuxArm64()

            targets.flatMap { it.compilations }
                .filterIsInstance<KotlinNativeCompilation>()
                .forEach { compilation -> compilation.cinterops.create("dummy") }
        }

        rootProject.enableCInteropCommonization()
        subproject.enableCInteropCommonization()

        rootProject.evaluate()
        subproject.evaluate()
    }

    @Test
    fun `test runCommonizer task`() {
        configureDefaultTestProjects()
        subproject.tasks.getByName("runCommonizer")
            .assertDependsOn(subproject.tasks.getByName("commonize"))
    }


    @Test
    fun `test commonizeNativeDistributionTask`() {
        configureDefaultTestProjects()
        val commonizeNativeDistributionTaskName = "commonizeNativeDistribution"
        /* Native Distribution Commonization should not be created in root project */
        rootProject.assertContainsNoTaskWithName(commonizeNativeDistributionTaskName)
        val commonizeNativeDistributionTask = subproject.assertContainsTaskWithName(commonizeNativeDistributionTaskName)
        subproject.tasks.getByName("commonize").assertDependsOn(commonizeNativeDistributionTask)
    }



    @Test
    fun `test commonizeNativeDistributionTask is not created eagerly`() {
        val project = buildProjectWithMPP {
            tasks.configureEach {
                if (it.name == "commonizeNativeDistribution") {
                    fail("Task $it was not expected to be created eagerly")
                }
            }

            kotlin {
                linuxArm64()
                linuxX64()
            }
        }

        project.evaluate()
    }


    @Test
    fun `test commonizeCInteropTask`() {
        configureDefaultTestProjects()
        val commonizeCInteropTaskName = "commonizeCInterop"
        val commonizeCInteropTask = subproject.assertContainsTaskWithName(commonizeCInteropTaskName)
        subproject.tasks.getByName("commonize").assertDependsOn(commonizeCInteropTask)
        rootProject.assertContainsNoTaskWithName(commonizeCInteropTaskName)
    }

    @Test
    fun `test applying CocoaPods plugin - enables commonization`() {
        val rootProject = ProjectBuilder.builder().build() as ProjectInternal
        rootProject.applyMultiplatformPlugin()

        rootProject.runLifecycleAwareTest {
            assertFalse(rootProject.cInteropCommonizationEnabled())

            rootProject.applyCocoapodsPlugin()

            assertTrue(rootProject.cInteropCommonizationEnabled())
        }
    }

    @Test
    fun `test applying CocoaPods plugin - in a root project - enables commonization only in the root project`() {
        val rootProject = ProjectBuilder.builder().build() as ProjectInternal
        val subproject = ProjectBuilder.builder().withParent(rootProject).build() as ProjectInternal
        rootProject.applyMultiplatformPlugin()
        subproject.applyMultiplatformPlugin()

        rootProject.runLifecycleAwareTest {
            assertFalse(rootProject.cInteropCommonizationEnabled())
            assertFalse(subproject.cInteropCommonizationEnabled())

            rootProject.applyCocoapodsPlugin()

            assertTrue(rootProject.cInteropCommonizationEnabled())
            assertFalse(subproject.cInteropCommonizationEnabled())
        }
    }

    @Test
    fun `test copyCommonizeCInteropForIdeTask creation - doesn't fail`() {
        val project = ProjectBuilder.builder().build()
        project.applyMultiplatformPlugin()
        project.enableCInteropCommonization(true)
        project.runLifecycleAwareTest {
            project.copyCommonizeCInteropForIdeTask()?.get()?.cInteropCommonizerTaskOutputDirectories
        }
    }

    @Test
    fun `test multi-module project - when commonization is enabled - expect commonizeCInterop task only registered in subprojects with native targets`() {
        val project = ProjectBuilder.builder()
            .withName("root")
            .build()

        with(project) {
            enableCInteropCommonization()
            (this as ProjectInternal).evaluate()
        }

        val subprojectWithNativeTarget = ProjectBuilder
            .builder()
            .withName("subprojectWithNativeTarget")
            .withParent(project)
            .build()
        with(subprojectWithNativeTarget) {
            applyMultiplatformPlugin()
            kotlin {
                jvm()
                js()
                linuxX64()
            }
            enableCInteropCommonization()
            (this as ProjectInternal).evaluate()
        }

        val subprojectWithoutNativeTarget = ProjectBuilder.builder()
            .withName("subprojectWithoutNativeTarget")
            .withParent(project)
            .build()
        with(subprojectWithoutNativeTarget) {
            applyMultiplatformPlugin()
            kotlin {
                jvm()
                js()
            }
            enableCInteropCommonization()
            (this as ProjectInternal).evaluate()
        }

        subprojectWithNativeTarget.runLifecycleAwareTest {
            assertContainsTaskWithName("commonizeCInterop")
        }
        subprojectWithoutNativeTarget.runLifecycleAwareTest {
            assertContainsNoTaskWithName("commonizeCInterop")
        }
    }
}
