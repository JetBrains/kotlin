/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.util.allCauses
import org.jetbrains.kotlin.gradle.util.assertContains
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.util.assertThrows
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

    @Test
    fun `language settings MonotonousCheck - language versions can be greater than root`() {
        val project = kmpProject {
            with(multiplatformExtension) {
                sourceSets.commonMain {
                    languageSettings.languageVersion = "1.4"
                }

                nonCommonMainSourceSets.all { srcSet ->
                    srcSet.languageSettings.languageVersion = "1.5"
                }
            }
        }

        project.evaluate()
    }

    @Test
    fun `language settings MonotonousCheck - language versions in leaves must be greater or equal than roots`() {
        val project = kmpProject {
            with(multiplatformExtension) {
                sourceSets.commonMain {
                    languageSettings.languageVersion = "1.4"
                }

                nonCommonMainSourceSets.all { srcSet ->
                    srcSet.languageSettings.languageVersion = "1.3"
                }
            }
        }

        val configException = assertThrows<ProjectConfigurationException> { project.evaluate() }
        val dataException = configException.allCauses.filterIsInstance<InvalidUserDataException>().single()
        assertContains(
            expected = "The language version of the dependent source set must be greater than or equal to that of its dependency.",
            actual = dataException.message.toString(),
        )
    }

    @Test
    fun `language settings MonotonousCheck - unstable features`() {
        val project = kmpProject {
            with(multiplatformExtension) {
                sourceSets.commonMain {
                    languageSettings.enableLanguageFeature("InlineClasses")
                }
            }
        }

        val configException = assertThrows<ProjectConfigurationException> { project.evaluate() }
        val dataException = configException.allCauses.filterIsInstance<InvalidUserDataException>().single()
        assertContains(
            expected = "The dependent source set must enable all unstable language features that its dependency has.",
            actual = dataException.message.toString(),
        )
    }

    @Test
    fun `language settings MonotonousCheck - opt-in`() {
        val project = kmpProject {
            with(multiplatformExtension) {
                sourceSets.commonMain {
                    languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
                }
            }
        }

        val configException = assertThrows<ProjectConfigurationException> { project.evaluate() }
        val dataException = configException.allCauses.filterIsInstance<InvalidUserDataException>().single()
        assertContains(
            expected = "The dependent source set must use all opt-in annotations that its dependency uses.",
            actual = dataException.message.toString(),
        )
    }

    /**
     * Check that enabling a bugfix feature and progressive mode or advancing API level
     * don't require doing the same for dependent source sets.
     */
    @Test
    fun `language settings MonotonousCheck - progressive mode`() {
        val project = kmpProject {
            with(multiplatformExtension) {
                sourceSets.commonMain {
                    languageSettings {
                        apiVersion = "1.4"
                        languageVersion = "1.3"
                        enableLanguageFeature("SoundSmartcastForEnumEntries")
                        progressiveMode = true
                    }
                }

                nonCommonMainSourceSets.all { srcSet ->
                    srcSet.languageSettings.languageVersion = "1.3"
                }
            }
        }

        project.evaluate()
    }

    companion object {
        private fun kmpProject(
            configure: Project.() -> Unit = {},
        ): ProjectInternal {
            return buildProjectWithMPP {
                with(multiplatformExtension) {
                    jvm()

                    js {
                        browser()
                        nodejs()
                    }
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmJs {
                        browser()
                        nodejs()
                    }

                    linuxX64()
                    linuxArm64()
                    mingwX64()
                    macosX64()
                    macosArm64()

                    applyDefaultHierarchyTemplate()
                }

                configure()
            }
        }

        private val KotlinMultiplatformExtension.nonCommonMainSourceSets: NamedDomainObjectSet<KotlinSourceSet>
            get() {
                val nonCommonMainSources = sourceSets.matching { it.name != sourceSets.commonMain.name }
                require(nonCommonMainSources.size > 0) {
                    "Expected that project ${project.name} has non-common main sources, but found none. All sourceSet names: ${sourceSets.names}"
                }
                return nonCommonMainSources
            }
    }
}
