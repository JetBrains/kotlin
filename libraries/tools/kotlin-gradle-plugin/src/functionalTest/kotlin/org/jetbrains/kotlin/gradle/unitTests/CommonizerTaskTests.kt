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

    private val rootProject = ProjectBuilder.builder().build() as ProjectInternal
    private val subproject = ProjectBuilder.builder().withName("subproject").withParent(rootProject).build() as ProjectInternal

    companion object {
        const val JVM_ECOSYSTEM_PLUGIN_ID = "jvm-ecosystem"
    }

    init {
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
        subproject.tasks.getByName("runCommonizer")
            .assertDependsOn(subproject.tasks.getByName("commonize"))

        /*
        Since commonizing the native distribution is done on the root project,
        we can also expect that the umbrella tasks are present there as well!
         */
        rootProject.tasks.getByName("runCommonizer")
            .assertDependsOn(rootProject.tasks.getByName("commonize"))
    }

    @Test
    fun `test commonizeNativeDistributionTask`() {
        val commonizeNativeDistributionTaskName = "commonizeNativeDistribution"
        subproject.assertContainsNoTaskWithName(commonizeNativeDistributionTaskName)

        /* Native Distribution Commonization is only done on the root project */
        val rootProjectCommonizeNativeDistributionTask = rootProject.assertContainsTaskWithName(commonizeNativeDistributionTaskName)
        rootProject.tasks.getByName("commonize").assertDependsOn(rootProjectCommonizeNativeDistributionTask)
        subproject.tasks.getByName("commonize").assertDependsOn(rootProjectCommonizeNativeDistributionTask)
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

    /**
     * Check if jvm-ecosystem plugin is applied when commonizer task is applied to the root project.
     * Context: https://github.com/gradle/gradle/issues/20145
     * https://youtrack.jetbrains.com/issue/KT-51583
     */
    @Test
    fun `test commonizeNativeDistributionTask applied jvm-ecosystem plugin`() {
        val rootProject = ProjectBuilder.builder().build() as ProjectInternal
        val subproject = ProjectBuilder.builder().withParent(rootProject).build() as ProjectInternal

        val jvmEcosystemPluginId = "jvm-ecosystem"
        assertNull(rootProject.plugins.findPlugin(jvmEcosystemPluginId))

        val kotlin = subproject.applyMultiplatformPlugin()
        assertNull(rootProject.plugins.findPlugin(jvmEcosystemPluginId))

        kotlin.linuxArm64()
        kotlin.linuxX64()

        rootProject.evaluate()
        subproject.evaluate()

        assertNotNull(rootProject.plugins.findPlugin(jvmEcosystemPluginId))
    }

    @Test
    fun `test commonizeNativeDistributionTask applied jvm-ecosystem plugin - jvm-ecosystem plugin is already applied`() {
        val rootProject = ProjectBuilder.builder().build() as ProjectInternal
        val subproject = ProjectBuilder.builder().withParent(rootProject).build() as ProjectInternal

        rootProject.plugins.apply(JVM_ECOSYSTEM_PLUGIN_ID)
        assertNotNull(rootProject.plugins.findPlugin(JVM_ECOSYSTEM_PLUGIN_ID))

        val kotlin = subproject.applyMultiplatformPlugin()

        kotlin.linuxArm64()
        kotlin.linuxX64()

        rootProject.evaluate()
        subproject.evaluate()

        assertNotNull(rootProject.plugins.findPlugin(JVM_ECOSYSTEM_PLUGIN_ID))
    }

    @Test
    fun `test commonizeNativeDistributionTask applied jvm-ecosystem plugin - java plugin is already applied`() {
        val rootProject = ProjectBuilder.builder().build() as ProjectInternal
        val subproject = ProjectBuilder.builder().withParent(rootProject).build() as ProjectInternal

        rootProject.plugins.apply("java")
        assertNotNull(rootProject.plugins.findPlugin(JVM_ECOSYSTEM_PLUGIN_ID))

        val kotlin = subproject.applyMultiplatformPlugin()

        kotlin.linuxArm64()
        kotlin.linuxX64()

        rootProject.evaluate()
        subproject.evaluate()

        assertNotNull(rootProject.plugins.findPlugin(JVM_ECOSYSTEM_PLUGIN_ID))
    }

    @Test
    fun `test commonizeCInteropTask`() {
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
}
