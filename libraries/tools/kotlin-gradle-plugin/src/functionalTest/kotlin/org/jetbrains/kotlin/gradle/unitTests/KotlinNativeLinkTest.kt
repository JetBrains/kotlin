/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

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
            repositories.mavenLocal()
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
                apiInCommon.layout.buildDirectory.file("classes/kotlin/linuxArm64/main/klib/apiInCommon.klib").get().asFile,
                apiInLinux.layout.buildDirectory.file("classes/kotlin/linuxArm64/main/klib/apiInLinux.klib").get().asFile,
            ),
            apiFiles.files,
        )
    }

}