/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createExternalKotlinTarget
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile as KotlinJvmCompileTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertTrue
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectCompilerOptionsTests {

    @Test
    fun nativeTargetCompilerOptionsDSL() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                linuxX64 {
                    compilerOptions {
                        progressiveMode.set(true)
                    }
                }

                iosX64 {
                    compilerOptions {
                        suppressWarnings.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        assertEquals(true, project.kotlinNativeTask("compileKotlinLinuxX64").compilerOptions.progressiveMode.get())
        assertEquals(false, project.kotlinNativeTask("compileKotlinLinuxX64").compilerOptions.suppressWarnings.get())
        assertEquals(true, project.kotlinNativeTask("compileTestKotlinLinuxX64").compilerOptions.progressiveMode.get())
        assertEquals(false, project.kotlinNativeTask("compileTestKotlinLinuxX64").compilerOptions.suppressWarnings.get())
        assertEquals(true, project.kotlinNativeTask("compileKotlinIosX64").compilerOptions.suppressWarnings.get())
        assertEquals(false, project.kotlinNativeTask("compileKotlinIosX64").compilerOptions.progressiveMode.get())
        assertEquals(true, project.kotlinNativeTask("compileTestKotlinIosX64").compilerOptions.suppressWarnings.get())
        assertEquals(false, project.kotlinNativeTask("compileTestKotlinIosX64").compilerOptions.progressiveMode.get())
    }

    @Test
    fun nativeTaskOverridesTargetOptions() {
        val project = buildProjectWithMPP {
            tasks.withType<KotlinCompilationTask<*>>().configureEach {
                if (it.name == "compileKotlinLinuxX64") {
                    it.compilerOptions.progressiveMode.set(false)
                }
            }

            with(multiplatformExtension) {
                linuxX64 {
                    compilerOptions {
                        progressiveMode.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        assertEquals(false, project.kotlinNativeTask("compileKotlinLinuxX64").compilerOptions.progressiveMode.get())
    }

    @Test
    fun nativeLanguageSettingsOverridesTargetOptions() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                linuxX64 {
                    compilerOptions {
                        progressiveMode.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()

                sourceSets.getByName("linuxX64Main").languageSettings {
                    progressiveMode = false
                }
            }
        }

        project.evaluate()

        assertEquals(false, project.kotlinNativeTask("compileKotlinLinuxX64").compilerOptions.progressiveMode.get())
    }

    @Test
    fun jvmTargetCompilerOptionsDSL() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm {
                    compilerOptions {
                        noJdk.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        assertEquals(true, project.kotlinJvmTask("compileKotlinJvm").compilerOptions.noJdk.get())
        assertEquals(true, project.kotlinJvmTask("compileTestKotlinJvm").compilerOptions.noJdk.get())
    }

    @Test
    fun jvmTaskOverridesTargetOptions() {
        val project = buildProjectWithMPP {
            tasks.withType<KotlinCompilationTask<*>>().configureEach {
                if (it.name == "compileKotlinJvm") {
                    it.compilerOptions.progressiveMode.set(false)
                }
            }

            with(multiplatformExtension) {
                jvm {
                    compilerOptions {
                        progressiveMode.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        assertEquals(false, project.kotlinJvmTask("compileKotlinJvm").compilerOptions.progressiveMode.get())
    }

    @Test
    fun jvmLanguageSettingsOverridesTargetOptions() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm {
                    compilerOptions {
                        progressiveMode.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()

                sourceSets.getByName("jvmMain").languageSettings {
                    progressiveMode = false
                }
            }
        }

        project.evaluate()

        assertEquals(false, project.kotlinJvmTask("compileKotlinJvm").compilerOptions.progressiveMode.get())
    }

    @Test
    fun jsTargetCompilerOptionsDsl() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                js {
                    compilerOptions {
                        suppressWarnings.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        assertEquals(true, project.kotlinJsTask("compileKotlinJs").compilerOptions.suppressWarnings.get())
        assertEquals(true, project.kotlinJsTask("compileTestKotlinJs").compilerOptions.suppressWarnings.get())
    }

    @Test
    fun jsTaskOptionsOverridesTargetOptions() {
        val project = buildProjectWithMPP {
            tasks.withType<KotlinCompilationTask<*>>().configureEach {
                if (it.name == "compileKotlinJs") {
                    it.compilerOptions.suppressWarnings.set(false)
                }
            }

            with(multiplatformExtension) {
                js {
                    compilerOptions {
                        suppressWarnings.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        assertEquals(false, project.kotlinJsTask("compileKotlinJs").compilerOptions.suppressWarnings.get())
    }

    @Test
    fun jsLanguageSettingsOverridesTargetOptions() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                js {
                    compilerOptions {
                        progressiveMode.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()

                sourceSets.getByName("jsMain").languageSettings {
                    progressiveMode = false
                }
            }
        }

        project.evaluate()

        assertEquals(false, project.kotlinJsTask("compileKotlinJs").compilerOptions.progressiveMode.get())
    }

    @Test
    fun metadataTargetDsl() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                linuxX64()
                iosX64()
                iosArm64()

                targets.named("metadata", KotlinMetadataTarget::class.java) {
                    it.compilerOptions {
                        progressiveMode.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        assertEquals(true, project.kotlinCommonTask("compileKotlinMetadata").compilerOptions.progressiveMode.get())
    }

    @Test
    fun metadataTaskOptionsOverrideTargetOptions() {
        val project = buildProjectWithMPP {
            tasks.withType<KotlinCompilationTask<*>>().configureEach {
                if (it.name == "compileKotlinMetadata") {
                    it.compilerOptions.progressiveMode.set(false)
                }
            }

            with(multiplatformExtension) {
                linuxX64()
                iosX64()
                iosArm64()

                targets.named("metadata", KotlinMetadataTarget::class.java) {
                    it.compilerOptions {
                        progressiveMode.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        assertEquals(false, project.kotlinCommonTask("compileKotlinMetadata").compilerOptions.progressiveMode.get())
    }

    @Test
    fun externalTargetDsl() {
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            with(multiplatformExtension) {
                val target = createExternalKotlinTarget<FakeTarget> { defaults() }
                target.createCompilation<FakeCompilation> { defaults(this@with) }
                target.createCompilation<FakeCompilation> { defaults(this@with, "test") }

                target.compilerOptions {
                    javaParameters.set(true)
                }
            }
        }

        assertEquals(true, project.kotlinJvmTask("compileKotlinFake").compilerOptions.javaParameters.get())
        assertEquals(true, project.kotlinJvmTask("compileTestKotlinFake").compilerOptions.javaParameters.get())
    }

    @Test
    fun externalTaskOptionsOverridesTargetOptions() {
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            tasks.withType<KotlinJvmCompileTask>().configureEach {
                if (it.name == "compileKotlinFake") {
                    it.compilerOptions.javaParameters.set(false)
                }
            }

            with(multiplatformExtension) {
                val target = createExternalKotlinTarget<FakeTarget> { defaults() }
                target.createCompilation<FakeCompilation> { defaults(this@with) }
                target.createCompilation<FakeCompilation> { defaults(this@with, "test") }

                target.compilerOptions {
                    javaParameters.set(true)
                }
            }
        }

        assertEquals(false, project.kotlinJvmTask("compileKotlinFake").compilerOptions.javaParameters.get())
        assertEquals(true, project.kotlinJvmTask("compileTestKotlinFake").compilerOptions.javaParameters.get())
    }

    @Test
    fun topLevelOptions() {
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            with(multiplatformExtension) {
                compilerOptions {
                    progressiveMode.set(true)
                }

                jvm()
                js()
                iosArm64()
                linuxX64()
            }
        }

        assertEquals(true, project.kotlinCommonTask("compileKotlinMetadata").compilerOptions.progressiveMode.get())
        assertEquals(true, project.kotlinCommonTask("compileNativeMainKotlinMetadata").compilerOptions.progressiveMode.get())
        assertEquals(true, project.kotlinCommonTask("compileCommonMainKotlinMetadata").compilerOptions.progressiveMode.get())
        assertEquals(true, project.kotlinJvmTask("compileKotlinJvm").compilerOptions.progressiveMode.get())
        assertEquals(true, project.kotlinJvmTask("compileTestKotlinJvm").compilerOptions.progressiveMode.get())
        assertEquals(true, project.kotlinJsTask("compileKotlinJs").compilerOptions.progressiveMode.get())
        assertEquals(true, project.kotlinJsTask("compileTestKotlinJs").compilerOptions.progressiveMode.get())
        assertEquals(true, project.kotlinNativeTask("compileKotlinLinuxX64").compilerOptions.progressiveMode.get())
        assertEquals(true, project.kotlinNativeTask("compileTestKotlinLinuxX64").compilerOptions.progressiveMode.get())
        assertEquals(true, project.kotlinNativeTask("compileKotlinIosArm64").compilerOptions.progressiveMode.get())
        assertEquals(true, project.kotlinNativeTask("compileTestKotlinIosArm64").compilerOptions.progressiveMode.get())
    }

    @Test
    fun testTargetDslOverridesTopLevelDsl() {
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            with(multiplatformExtension) {
                jvm {
                    compilerOptions {
                        languageVersion.set(KotlinVersion.KOTLIN_1_8)
                    }
                }

                compilerOptions {
                    languageVersion.set(KotlinVersion.KOTLIN_2_0)
                }
            }
        }

        assertEquals(KotlinVersion.KOTLIN_1_8, project.kotlinJvmTask("compileKotlinJvm").compilerOptions.languageVersion.orNull)
        assertEquals(KotlinVersion.KOTLIN_1_8, project.kotlinJvmTask("compileTestKotlinJvm").compilerOptions.languageVersion.orNull)
    }

    @Test
    fun testTaskDslOverrideTopLevelDsl() {
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            tasks.withType<KotlinJvmCompileTask>().configureEach {
                if (it.name == "compileKotlinJvm") {
                    it.compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_1_8)
                }
            }

            with(multiplatformExtension) {
                jvm()

                compilerOptions {
                    languageVersion.set(KotlinVersion.KOTLIN_2_0)
                }
            }
        }

        assertEquals(KotlinVersion.KOTLIN_1_8, project.kotlinJvmTask("compileKotlinJvm").compilerOptions.languageVersion.orNull)
    }

    @Test
    fun testTopLevelDslAlsoConfiguresSharedSourceSets() {
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            with(multiplatformExtension) {
                jvm()
                js()

                compilerOptions {
                    languageVersion.set(KotlinVersion.KOTLIN_2_0)
                }
            }
        }

        assertEquals("2.0", project.multiplatformExtension.sourceSets.getByName("commonTest").languageSettings.languageVersion)
    }

    @Test
    fun testJvmModuleNameInMppIsConfigured() {
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            with(multiplatformExtension) {
                jvm {
                    compilerOptions.moduleName.set("my-custom-module-name")
                }
            }
        }

        assertEquals("my-custom-module-name", project.kotlinJvmTask("compileKotlinJvm").compilerOptions.moduleName.orNull)
    }

    @Test
    fun testJsOptionsIsConfigured() {
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            with(multiplatformExtension) {
                js {
                    compilerOptions {
                        moduleName.set("js-module-name")
                        freeCompilerArgs.add("-Xstrict-implicit-export-types")
                    }
                }
            }
        }

        val kotlinJsCompileTask = project.kotlinJsTask("compileKotlinJs")
        assertEquals("js-module-name", kotlinJsCompileTask.compilerOptions.moduleName.orNull)
        assertTrue(
            kotlinJsCompileTask
                .compilerOptions
                .freeCompilerArgs.get()
                .contains("-Xstrict-implicit-export-types")
        )
    }

    @Test
    fun testJsBrowserConfigDoesNotOverrideFreeCompilerArgsFromTarget() {
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            with(multiplatformExtension) {
                js {
                    binaries.executable()
                    browser()
                    compilerOptions {
                        freeCompilerArgs.addAll("-Xstrict-implicit-export-types", "-Xexplicit-api=warning")
                    }
                }
            }
        }

        val kotlinJsCompileTask = project.kotlinJsTask("compileKotlinJs")
        assertTrue(
            (kotlinJsCompileTask as Kotlin2JsCompile)
                .enhancedFreeCompilerArgs.get()
                .containsAll(listOf("-Xstrict-implicit-export-types", "-Xexplicit-api=warning"))
        )
    }

    @Test
    fun testJsLinkTaskAreAlsoConfiguredWithOptionsFromDSL() {
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            with(multiplatformExtension) {
                js {
                    nodejs()
                    binaries.library()
                    compilerOptions {
                        moduleName.set("my-custom-module")
                        languageVersion.set(KotlinVersion.KOTLIN_1_8)
                        apiVersion.set(KotlinVersion.KOTLIN_1_8)
                        moduleKind.set(JsModuleKind.MODULE_PLAIN)
                        freeCompilerArgs.addAll("-Xstrict-implicit-export-types", "-Xexplicit-api=warning")
                    }
                }
            }
        }

        val jsTasks = listOf(
            project.kotlinJsTask("compileKotlinJs"),
            project.kotlinJsTask("compileDevelopmentLibraryKotlinJs"),
            project.kotlinJsTask("compileProductionLibraryKotlinJs")
        )
        jsTasks.forEach { jsTask ->
            if (jsTask.name == "compileKotlinJs") {
                // JS IR has different module name from 'compileKotlinJs'
                assertEquals("my-custom-module", jsTask.compilerOptions.moduleName.orNull)
            }
            assertEquals(KotlinVersion.KOTLIN_1_8, jsTask.compilerOptions.languageVersion.orNull)
            assertEquals(KotlinVersion.KOTLIN_1_8, jsTask.compilerOptions.apiVersion.orNull)
            assertEquals(JsModuleKind.MODULE_PLAIN, jsTask.compilerOptions.moduleKind.orNull)
            assertTrue(
                jsTask.compilerOptions.freeCompilerArgs.get().containsAll(
                    listOf("-Xstrict-implicit-export-types", "-Xexplicit-api=warning")
                )
            )
        }
    }

    private fun Project.kotlinNativeTask(name: String): KotlinCompilationTask<KotlinNativeCompilerOptions> = tasks
        .named<KotlinCompilationTask<KotlinNativeCompilerOptions>>(name)
        .get()

    private fun Project.kotlinJsTask(name: String): KotlinCompilationTask<KotlinJsCompilerOptions> = tasks
        .named<KotlinCompilationTask<KotlinJsCompilerOptions>>(name)
        .get()

    private fun Project.kotlinJvmTask(name: String): KotlinCompilationTask<KotlinJvmCompilerOptions> = tasks
        .named<KotlinCompilationTask<KotlinJvmCompilerOptions>>(name)
        .get()

    private fun Project.kotlinCommonTask(name: String): KotlinCompilationTask<KotlinCommonCompilerOptions> = tasks
        .named<KotlinCompilationTask<KotlinCommonCompilerOptions>>(name)
        .get()
}
