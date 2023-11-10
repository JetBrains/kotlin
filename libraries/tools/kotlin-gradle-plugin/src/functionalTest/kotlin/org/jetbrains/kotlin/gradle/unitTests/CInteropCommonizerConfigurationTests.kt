/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.linuxMain
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.ReadyForExecution
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.targets.native.internal.CommonizerTargetAttribute
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrCreateCommonizedCInteropDependencyConfiguration
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.core.UnsafeApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class CInteropCommonizerConfigurationTests {

    @Test
    fun `test - compatibility rule - superset is compatible`() {
        val project = buildProjectWithMPP()

        val consumable = project.configurations.createConsumable("testElements").also { configuration ->
            configuration.attributes.attribute(
                CommonizerTargetAttribute.attribute,
                CommonizerTarget(
                    KonanTarget.LINUX_X64,
                    KonanTarget.LINUX_ARM64,
                    KonanTarget.IOS_ARM64,
                    KonanTarget.IOS_X64
                ).identityString
            )
        }

        val resolvable = project.configurations.createResolvable("testDependencies").also { configuration ->
            configuration.attributes.attribute(
                CommonizerTargetAttribute.attribute,
                CommonizerTarget(
                    KonanTarget.IOS_ARM64,
                    KonanTarget.IOS_X64
                ).identityString
            )
        }

        project.dependencies {
            resolvable(project)
        }

        val resolvedDependencies = resolvable.resolvedConfiguration.lenientConfiguration.allModuleDependencies
        if (resolvedDependencies.isEmpty()) fail("Expected at least one dependency")
        if (resolvedDependencies.size > 1) fail("Expected exactly one dependency. Found: $resolvedDependencies")
        val resolved = resolvedDependencies.single()
        assertEquals(consumable.name, resolved.configuration)
    }

    @Test
    fun `test - disambiguation rule - chooses most specific variant`() {
        val project = buildProjectWithMPP()

        project.configurations.createConsumable("testConsumableAll").also { configuration ->
            configuration.attributes.attribute(
                CommonizerTargetAttribute.attribute,
                CommonizerTarget(
                    KonanTarget.LINUX_X64,
                    KonanTarget.LINUX_ARM64,
                    KonanTarget.IOS_ARM64,
                    KonanTarget.IOS_X64,
                    KonanTarget.MACOS_X64,
                    KonanTarget.MACOS_ARM64
                ).identityString
            )
        }

        /* More specific as it does not offer macos parts */
        val consumableSpecific = project.configurations.createConsumable("testConsumableSpecific").also { configuration ->
            configuration.attributes.attribute(
                CommonizerTargetAttribute.attribute,
                CommonizerTarget(
                    KonanTarget.IOS_ARM64,
                    KonanTarget.IOS_X64,
                    KonanTarget.LINUX_X64,
                    KonanTarget.LINUX_ARM64,
                ).identityString
            )
        }

        val resolvable = project.configurations.createResolvable("testDependencies").also { configuration ->
            configuration.attributes.attribute(
                CommonizerTargetAttribute.attribute,
                CommonizerTarget(
                    KonanTarget.IOS_ARM64,
                    KonanTarget.IOS_X64
                ).identityString
            )
        }

        project.dependencies {
            resolvable(project)
        }

        val resolvedDependencies = resolvable.resolvedConfiguration.lenientConfiguration.allModuleDependencies
        if (resolvedDependencies.isEmpty()) fail("Expected at least one dependency")
        if (resolvedDependencies.size > 1) fail("Expected exactly one dependency. Found: $resolvedDependencies")
        val resolved = resolvedDependencies.single()
        assertEquals(consumableSpecific.name, resolved.configuration)
    }

    @Test
    fun `test - KT-63338 - source sets have own cinterop configurations`() {
        val rootProject = buildProject()

        val producerAProject = buildProjectWithMPP(
            projectBuilder = { withParent(rootProject).withName("producer-a") }
        ) {
            kotlin {
                linuxX64().compilations.main.cinterops.create("myInterop")
                linuxArm64().compilations.main.cinterops.create("myInterop")
            }
        }

        val producerBProject = buildProjectWithMPP(
            projectBuilder = { withParent(rootProject).withName("producer-b") }
        ) {
            kotlin {
                linuxX64().compilations.main.cinterops.create("myInterop")
                linuxArm64().compilations.main.cinterops.create("myInterop")
            }
        }


        val consumerProject = buildProjectWithMPP(
            projectBuilder = { withParent(rootProject).withName("consumer") }
        ) {
            kotlin {
                linuxX64()
                linuxArm64()

                sourceSets.commonMain.dependencies {
                    implementation(project(":producer-a"))
                }

                sourceSets.linuxMain.dependencies {
                    implementation(project(":producer-b"))
                }
            }
        }

        producerAProject.evaluate()

        consumerProject.runLifecycleAwareTest {
            val commonMain = consumerProject.multiplatformExtension.sourceSets.commonMain
            val linuxMain = consumerProject.multiplatformExtension.sourceSets.linuxMain

            @OptIn(UnsafeApi::class)
            val commonMainCinterop = consumerProject.locateOrCreateCommonizedCInteropDependencyConfiguration(commonMain.get())!!

            @OptIn(UnsafeApi::class)
            val linuxMainCinterop = consumerProject.locateOrCreateCommonizedCInteropDependencyConfiguration(linuxMain.get())!!

            if (linuxMainCinterop == commonMainCinterop)
                fail("Expected different source sets have different resolvable Cinterop configurations")

            fun Configuration.allProjectDependencies() = allDependencies
                .filterIsInstance<ProjectDependency>()
                .map { it.dependencyProject }
                .toSet()

            if (commonMainCinterop.allProjectDependencies() != setOf(producerAProject))
                fail("commonMain Cinterop configuration should depend only on `producer-a` project")

            if (linuxMainCinterop.allProjectDependencies() != setOf(producerAProject, producerBProject))
                fail("linuxMain Cinterop configuration should depend on both `producer-a` and `producer-b` projects")
        }
    }
}