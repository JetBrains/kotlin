/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.tasks.DefaultTaskContainer
import org.gradle.api.tasks.TaskInstantiationException
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.kotlinBuildDeps
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_LINK_MAX_WORKERS
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.isParallelismLimitServiceRegistered
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.maxParallelUsagesOf
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.jetbrains.kotlin.util.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class KotlinNativeLinkTest {

    @Test
    fun `apiFiles - contain api dependencies - when KotlinNativeLink task is configured before compilation's api dependencies`() {
        val parent = buildProject()
        val apiInCommon = buildProjectWithMPP(
            projectBuilder = { withParent(parent).withName("apiInCommon") }
        ) { kotlin { linuxArm64() } }.evaluate()

        val apiInLinux = buildProjectWithMPP(
            projectBuilder = { withParent(parent).withName("apiInLinux") }
        ) { kotlin { linuxArm64() } }.evaluate()

        buildProjectWithMPP(
            projectBuilder = { withParent(parent).withName("apiInIos") }
        ) { kotlin { linuxArm64() } }.evaluate()

        val exportingProject = buildProjectWithMPP(
            projectBuilder = { withParent(parent).withName("exportingProject") }
        ) {
            repositories.kotlinBuildDeps()
            repositories.mavenCentralCacheRedirector()
            kotlin {
                iosArm64()
                linuxArm64 {
                    binaries.staticLib {
                        export(project(":apiInCommon"))
                        export(project(":apiInLinux"))
                    }
                }

                sourceSets.commonMain.dependencies { api(project(":apiInCommon")) }
                sourceSets.linuxMain.dependencies { api(project(":apiInLinux")) }
                sourceSets.iosMain.dependencies { api(project(":apiInIos")) }
            }
        }

        // 1. Configure KotlinNativeLink's apiFiles before compilation's apiConfiguration is wired. Using apiFilesConfiguration directly here because apiFiles is filtered by File.exists check
        val apiFiles = (exportingProject.tasks.getByName("linkReleaseStaticLinuxArm64") as KotlinNativeLink).apiFiles

        // 2. Set up the compilations
        exportingProject.evaluate()

        assertEquals(
            hashSetOf(
                apiInCommon.layout.buildDirectory.file("classes/kotlin/linuxArm64/main/klib/apiInCommon").get().asFile,
                apiInLinux.layout.buildDirectory.file("classes/kotlin/linuxArm64/main/klib/apiInLinux").get().asFile,
            ),
            apiFiles.files,
        )
    }

    @Test
    fun `KT-72112 - eager KotlinNativeLink task instantiation - doesn't fail configuration`() {
        buildProjectWithMPP {
            // Force all tasks to instantiate eagerly
            tasks.all { }
            kotlin {
                iosSimulatorArm64().binaries.framework { }
            }
        }.evaluate()
    }

    @Test
    fun `KotlinNativeLink task configures the parallelism limit service using the KOTLIN_NATIVE_LINK_MAX_WORKERS property, when set`() {
        val project = buildProjectWithMPP(
            preApplyCode = { propertiesExtension.set(KOTLIN_NATIVE_LINK_MAX_WORKERS, "3") }
        ) {
            // Force all tasks to instantiate eagerly, so the KotlinNativeLink's configureEach actions run
            tasks.all { }
            kotlin {
                iosSimulatorArm64().binaries.framework { }
            }
        }
        project.evaluate()

        assertEquals(3, project.maxParallelUsagesOf<KotlinNativeLink>())
    }

    @Test
    fun `KotlinNativeLink task does not register the parallelism limit service when the KOTLIN_NATIVE_LINK_MAX_WORKERS property is not set`() {
        val project = buildProjectWithMPP {
            // Force all tasks to instantiate eagerly, so the KotlinNativeLink's configureEach actions run
            tasks.all { }
            kotlin {
                iosSimulatorArm64().binaries.framework { }
            }
        }
        project.evaluate()

        assertFalse(project.isParallelismLimitServiceRegistered<KotlinNativeLink>())
    }

}
