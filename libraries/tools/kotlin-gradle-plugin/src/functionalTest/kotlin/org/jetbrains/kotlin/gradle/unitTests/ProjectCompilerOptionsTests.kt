/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.config.LanguageVersion
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

                @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
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
                @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
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
                @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
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
        val compileKotlinVersion = LanguageVersion.FIRST_NON_DEPRECATED.asKotlinVersion()
        val compileTestKotlinVersion = LanguageVersion.LATEST_STABLE.asKotlinVersion()
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            with(multiplatformExtension) {
                jvm {
                    compilerOptions {
                        languageVersion.set(compileKotlinVersion)
                    }
                }

                compilerOptions {
                    languageVersion.set(compileTestKotlinVersion)
                }
            }
        }

        assertEquals(compileKotlinVersion, project.kotlinJvmTask("compileKotlinJvm").compilerOptions.languageVersion.orNull)
        assertEquals(compileKotlinVersion, project.kotlinJvmTask("compileTestKotlinJvm").compilerOptions.languageVersion.orNull)
    }

    @Test
    fun testTaskDslOverrideTopLevelDsl() {
        val compileTaskKotlinVersion = LanguageVersion.FIRST_NON_DEPRECATED.asKotlinVersion()
        val compileTopLevelKotlinVersion = LanguageVersion.LATEST_STABLE.asKotlinVersion()
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            tasks.withType<KotlinJvmCompileTask>().configureEach {
                if (it.name == "compileKotlinJvm") {
                    it.compilerOptions.languageVersion.set(compileTaskKotlinVersion)
                }
            }

            with(multiplatformExtension) {
                jvm()

                compilerOptions {
                    languageVersion.set(compileTopLevelKotlinVersion)
                }
            }
        }

        assertEquals(compileTaskKotlinVersion, project.kotlinJvmTask("compileKotlinJvm").compilerOptions.languageVersion.orNull)
    }

    @Test
    fun testTopLevelDslAlsoConfiguresSharedSourceSets() {
        val version = LanguageVersion.LATEST_STABLE
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            with(multiplatformExtension) {
                jvm()
                js()

                compilerOptions {
                    languageVersion.set(version.asKotlinVersion())
                }
            }
        }

        assertEquals(version.versionString, project.multiplatformExtension.sourceSets.getByName("commonTest").languageSettings.languageVersion)
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
        val version = LanguageVersion.LATEST_STABLE.asKotlinVersion()
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            with(multiplatformExtension) {
                js {
                    nodejs()
                    binaries.library()
                    compilerOptions {
                        moduleName.set("my-custom-module")
                        languageVersion.set(version)
                        apiVersion.set(version)
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
            assertEquals(version, jsTask.compilerOptions.languageVersion.orNull)
            assertEquals(version, jsTask.compilerOptions.apiVersion.orNull)
            assertEquals(JsModuleKind.MODULE_PLAIN, jsTask.compilerOptions.moduleKind.orNull)
            assertTrue(
                jsTask.compilerOptions.freeCompilerArgs.get().containsAll(
                    listOf("-Xstrict-implicit-export-types", "-Xexplicit-api=warning")
                )
            )
        }
    }

    @Test
    fun multiTargetCompilerOptionsAreIsolated() {
        // Config-phase port of KGP `CompilerOptionsProjectIT.mppCompilerOptionsDsl`: jvm, js and native are
        // configured simultaneously, and each compile task must end up with exactly its own options — top-level
        // values where the target didn't override them, the target's own value where it did, and nothing leaking
        // from a sibling target.
        //
        // `languageVersion`/`apiVersion` use two versions kept monotonic across the source-set hierarchy
        // (top-level is the lowest, jvm raises it) so the override is observable without tripping the source-set
        // consistency check. `progressiveMode`/`jvmTarget`/`friendModulesDisabled` are each set on a single target
        // and double as cross-target leak probes.
        val topLevelVersion = LanguageVersion.FIRST_NON_DEPRECATED.asKotlinVersion()
        val jvmVersion = LanguageVersion.LATEST_STABLE.asKotlinVersion()
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                compilerOptions {
                    languageVersion.set(topLevelVersion)
                    apiVersion.set(topLevelVersion)
                }
                jvm {
                    compilerOptions {
                        languageVersion.set(jvmVersion)
                        apiVersion.set(jvmVersion)
                        jvmTarget.set(JvmTarget.JVM_11)
                        javaParameters.set(true)
                    }
                }
                js {
                    compilerOptions {
                        friendModulesDisabled.set(true)
                    }
                }
                linuxX64 {
                    compilerOptions {
                        progressiveMode.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        // The top-level options reach the common metadata compilation (restored from `mppCompilerOptionsDsl`).
        val common = project.kotlinCommonTask("compileCommonMainKotlinMetadata").compilerOptions
        assertEquals(topLevelVersion, common.languageVersion.orNull)
        assertEquals(topLevelVersion, common.apiVersion.orNull)

        val jvm = project.kotlinJvmTask("compileKotlinJvm").compilerOptions
        assertEquals(JvmTarget.JVM_11, jvm.jvmTarget.orNull)
        assertEquals(true, jvm.javaParameters.get())
        assertEquals(jvmVersion, jvm.languageVersion.orNull) // jvm's own value overrides the top-level one
        assertEquals(jvmVersion, jvm.apiVersion.orNull) // jvm's own value overrides the top-level one
        assertEquals(false, jvm.progressiveMode.get()) // native's progressiveMode must not leak here

        val js = project.kotlinJsTask("compileKotlinJs").compilerOptions
        assertEquals(true, js.friendModulesDisabled.get())
        assertEquals(topLevelVersion, js.languageVersion.orNull) // inherits top-level; jvm's override must not leak here
        assertEquals(false, js.progressiveMode.get()) // native's progressiveMode must not leak here

        val native = project.kotlinNativeTask("compileKotlinLinuxX64").compilerOptions
        assertEquals(topLevelVersion, native.languageVersion.orNull) // inherits top-level; jvm's override must not leak here
        assertEquals(true, native.progressiveMode.get())
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
