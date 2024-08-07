/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.artifacts.UklibArchiveTask
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableUklibs
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test

class UklibTests {

    @Test
    fun test() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                enableUklibs()
            }
        ) {
            kotlin {
                iosArm64()
                iosX64()
                macosArm64()
                linuxArm64()
                linuxX64()
                jvm()
            }
        }.evaluate()

        val model = (project.tasks.getByName("packUklib") as UklibArchiveTask).model.get()
        println(model)
    }

}