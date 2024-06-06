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
        val a = buildProjectWithMPP(
            projectBuilder = { withParent(parent).withName("a") }
        ) { kotlin { linuxArm64() } }.evaluate()

        val b = buildProjectWithMPP(
            projectBuilder = { withParent(parent).withName("b") }
        ) {
            repositories.mavenLocal()
            repositories.mavenCentralCacheRedirector()
            kotlin {
                linuxArm64 {
                    binaries.staticLib {
                        export(project(":a"))
                    }
                }

                sourceSets.commonMain.dependencies {
                    api(project(":a"))
                }
            }
        }

        // 1. Configure KotlinNativeLink's apiFiles before compilation's apiConfiguration is wired. Using apiFilesConfiguration directly here because apiFiles is filtered by File.exists check
        val apiFiles = (b.tasks.getByName("linkReleaseStaticLinuxArm64") as KotlinNativeLink).apiFiles

        // 2. Set up the compilations
        b.evaluate()

        assertEquals(
            hashSetOf(
                a.layout.buildDirectory.file("classes/kotlin/linuxArm64/main/klib/a.klib").get().asFile
            ),
            apiFiles.files,
        )
    }

}