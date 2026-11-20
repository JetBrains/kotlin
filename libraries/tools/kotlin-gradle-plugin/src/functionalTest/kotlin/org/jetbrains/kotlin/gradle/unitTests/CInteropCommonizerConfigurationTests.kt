/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.linuxMain
import org.jetbrains.kotlin.gradle.plugin.configurationResult
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

        val consumable = project.configurations.createConsumable("testElements") {
            attributes.attribute(
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

        project.dependencies.add(resolvable.name, project)

        val resolvedDependencies = resolvable.resolvedConfiguration.lenientConfiguration.allModuleDependencies
        if (resolvedDependencies.isEmpty()) fail("Expected at least one dependency")
        if (resolvedDependencies.size > 1) fail("Expected exactly one dependency. Found: $resolvedDependencies")
        val resolved = resolvedDependencies.single()
        assertEquals(consumable.name, resolved.configuration)
    }

    @Test
    fun `test - disambiguation rule - chooses most specific variant`() {
        val project = buildProjectWithMPP()

        project.configurations.createConsumable("testConsumableAll") {
            attributes.attribute(
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
        val consumableSpecific = project.configurations.createConsumable("testConsumableSpecific") {
            attributes.attribute(
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

        project.dependencies.add(resolvable.name, project)

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
        producerBProject.evaluate()

        consumerProject.runLifecycleAwareTest {
            // Dependencies of the cinterop configurations are derived from the resolved metadata dependencies
            configurationResult.await()

            val commonMain = consumerProject.multiplatformExtension.sourceSets.commonMain
            val linuxMain = consumerProject.multiplatformExtension.sourceSets.linuxMain

            @OptIn(UnsafeApi::class)
            val commonMainCinterop = consumerProject.locateOrCreateCommonizedCInteropDependencyConfiguration(commonMain.get())!!

            @OptIn(UnsafeApi::class)
            val linuxMainCinterop = consumerProject.locateOrCreateCommonizedCInteropDependencyConfiguration(linuxMain.get())!!

            if (linuxMainCinterop == commonMainCinterop)
                fail("Expected different source sets have different resolvable Cinterop configurations")

            fun Configuration.allProjectDependencies(): Set<Project?> = allDependencies
                .filterIsInstance<ProjectDependency>()
                .map { project.project(it.path) }
                .toSet()

            if (commonMainCinterop.allProjectDependencies() != setOf(producerAProject))
                fail("commonMain Cinterop configuration should depend only on `producer-a` project")

            if (linuxMainCinterop.allProjectDependencies() != setOf(producerAProject, producerBProject))
                fail("linuxMain Cinterop configuration should depend on both `producer-a` and `producer-b` projects")
        }
    }

    @Test
    fun `test - cinterop configuration - contains only project dependencies`() {
        val rootProject = buildProject()

        val producerProject = buildProjectWithMPP(
            projectBuilder = { withParent(rootProject).withName("producer") },
            preApplyCode = { enableCInteropCommonization() },
        ) {
            kotlin {
                linuxX64().compilations.main.cinterops.create("myInterop")
                linuxArm64().compilations.main.cinterops.create("myInterop")
            }
        }

        val consumerProject = buildProjectWithMPP(
            projectBuilder = { withParent(rootProject).withName("consumer") },
            preApplyCode = { enableCInteropCommonization() },
        ) {
            enableDependencyVerification(false)
            repositories.mavenCentralCacheRedirector()
            kotlin {
                linuxX64()
                linuxArm64()

                sourceSets.commonMain.dependencies {
                    implementation(project(":producer"))
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                }
            }
        }

        producerProject.evaluate()

        consumerProject.runLifecycleAwareTest {
            configurationResult.await()

            @OptIn(UnsafeApi::class)
            val commonMainCinterop = consumerProject.locateOrCreateCommonizedCInteropDependencyConfiguration(
                consumerProject.multiplatformExtension.sourceSets.commonMain.get()
            )!!

            assertEquals(
                setOf(":producer"),
                commonMainCinterop.allDependencies.filterIsInstance<ProjectDependency>().map { it.path }.toSet(),
            )
            assertEquals(
                emptyList(),
                commonMainCinterop.allDependencies.filterIsInstance<ExternalModuleDependency>().map { "${it.group}:${it.name}" },
            )
            assertEquals(
                emptyList(),
                commonMainCinterop.incoming.resolutionResult.allDependencies
                    .filterIsInstance<UnresolvedDependencyResult>()
                    .map { result -> generateSequence(result.failure) { it.cause }.joinToString(" <- ") { it.message.orEmpty() } },
            )
        }
    }

    @Test
    fun `test - cinterop configuration - contains transitive project dependencies`() {
        val rootProject = buildProject()

        val producerProject = buildProjectWithMPP(
            projectBuilder = { withParent(rootProject).withName("producer") },
            preApplyCode = { enableCInteropCommonization() },
        ) {
            kotlin {
                linuxX64().compilations.main.cinterops.create("myInterop")
                linuxArm64().compilations.main.cinterops.create("myInterop")
            }
        }

        val middleProject = buildProjectWithMPP(
            projectBuilder = { withParent(rootProject).withName("middle") },
            preApplyCode = { enableCInteropCommonization() },
        ) {
            kotlin {
                linuxX64()
                linuxArm64()

                sourceSets.commonMain.dependencies {
                    api(project(":producer"))
                }
            }
        }

        val consumerProject = buildProjectWithMPP(
            projectBuilder = { withParent(rootProject).withName("consumer") },
            preApplyCode = { enableCInteropCommonization() },
        ) {
            kotlin {
                linuxX64()
                linuxArm64()

                sourceSets.commonMain.dependencies {
                    implementation(project(":middle"))
                }
            }
        }

        producerProject.evaluate()
        middleProject.evaluate()

        consumerProject.runLifecycleAwareTest {
            configurationResult.await()

            @OptIn(UnsafeApi::class)
            val commonMainCinterop = consumerProject.locateOrCreateCommonizedCInteropDependencyConfiguration(
                consumerProject.multiplatformExtension.sourceSets.commonMain.get()
            )!!

            assertEquals(
                setOf(":middle", ":producer"),
                commonMainCinterop.allDependencies.filterIsInstance<ProjectDependency>().map { it.path }.toSet(),
            )
        }
    }
}
