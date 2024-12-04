/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.kotlinArtifacts
import org.jetbrains.kotlin.konan.target.KonanTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinNativeFatFrameworkTests {

    @Test
    fun `kotlinArtifacts universal framework task - keeps task dependencies on link tasks`() {
        val project = buildProjectWithMPP {
            kotlin {
                iosArm64()
                iosX64()
            }

            kotlinArtifacts {
                Native.FatFramework {
                    it.targets(
                        KonanTarget.IOS_ARM64,
                        KonanTarget.IOS_X64
                    )
                }
            }
        }.evaluate()

        assertEquals(
            setOf(
                project.tasks.getByName("assembleTestDebugFrameworkIosX64ForFat"),
                project.tasks.getByName("assembleTestDebugFrameworkIosArm64ForFat"),
            ),
            project.tasks.getByName("assembleTestDebugFatFramework").taskDependencies.getDependencies(null),
        )
    }

}