/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.junit.Test
import kotlin.test.assertEquals

class LanguageSettingsTests {

    @Test
    fun languageSettingsSyncToCompilerOptions() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()

                linuxX64()
                linuxArm64()

                iosX64()
                iosArm64()

                applyDefaultHierarchyTemplate()

                sourceSets.configureEach {
                    it.languageSettings {
                        apiVersion = "1.7"
                        languageVersion = "1.8"
                    }
                }
            }
        }

        project.evaluate()

        listOf(
            "compileCommonMainKotlinMetadata",
            "compileKotlinJvm",
            "compileNativeMainKotlinMetadata",
            "compileLinuxMainKotlinMetadata",
            "compileAppleMainKotlinMetadata",
            "compileIosMainKotlinMetadata",
            "compileKotlinLinuxX64",
            "compileKotlinLinuxArm64",
            "compileKotlinIosX64",
            "compileKotlinIosArm64"
        ).forEach { taskName ->
            val compileTask = project.tasks.getByName(taskName) as KotlinCompilationTask<*>
            with(compileTask.compilerOptions) {
                assertEquals(apiVersion.orNull, KotlinVersion.KOTLIN_1_7)
                assertEquals(languageVersion.orNull, KotlinVersion.KOTLIN_1_8)
            }
        }
    }

    @Test
    fun compilerOptionsSyncToLanguageSettings() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()

                linuxX64()
                linuxArm64()

                iosX64()
                iosArm64()

                applyDefaultHierarchyTemplate()
            }


            tasks.withType<KotlinCompilationTask<*>>().all {
                it.compilerOptions {
                    apiVersion.set(KotlinVersion.KOTLIN_1_7)
                    languageVersion.set(KotlinVersion.KOTLIN_1_8)
                }
            }
        }

        project.evaluate()

        listOf(
            "commonMain",
            "jvmMain",
            "nativeMain",
            "linuxMain",
            "appleMain",
            "iosMain",
            "linuxX64Main",
            "linuxArm64Main",
            "iosX64Main",
            "iosArm64Main",
        ).forEach { sourceSetName ->
            with(project.multiplatformExtension.sourceSets.getByName(sourceSetName).languageSettings) {
                assertEquals("1.7", apiVersion)
                assertEquals("1.8", languageVersion)
            }
        }
    }
}