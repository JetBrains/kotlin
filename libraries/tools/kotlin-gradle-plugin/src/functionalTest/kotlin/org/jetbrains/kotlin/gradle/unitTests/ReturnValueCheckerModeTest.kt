/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.ReturnValueCheckerMode
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * Configuration-phase verification of the return value checker `kotlin {}` DSL: the configured mode must end up as the
 * `-Xreturn-value-checker` compiler argument of the corresponding compile tasks. Unlike `explicitApi`, the production
 * mode applies to test compilations as well, unless a test-specific mode is configured. Verified by computing each
 * task's arguments at configuration time.
 */
@OptIn(ExperimentalKotlinGradlePluginApi::class)
class ReturnValueCheckerModeTest {

    @Test
    fun returnValueCheckerNotSetByDefault() {
        val project = buildProjectWithJvm()
        project.evaluate()

        assertFalse(
            project.compileArguments("compileKotlin").any { it.startsWith("-Xreturn-value-checker") },
            "the return value checker argument must not appear by default in the production compilation",
        )
        assertFalse(
            project.compileArguments("compileTestKotlin").any { it.startsWith("-Xreturn-value-checker") },
            "the return value checker argument must not appear by default in the test compilation",
        )
    }

    @Test
    fun returnValueCheckerAppliesToBothMainAndTest() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.returnValueChecker()
        project.evaluate()

        assertContains(project.compileArguments("compileKotlin"), "-Xreturn-value-checker=check")
        // the return value checker mode applies to test compilations by default
        assertContains(project.compileArguments("compileTestKotlin"), "-Xreturn-value-checker=check")
    }

    @Test
    fun returnValueCheckerSeparateTestMode() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.returnValueChecker(ReturnValueCheckerMode.Full, ReturnValueCheckerMode.Check)
        project.evaluate()

        assertContains(project.compileArguments("compileKotlin"), "-Xreturn-value-checker=full")
        val testArguments = project.compileArguments("compileTestKotlin")
        assertContains(testArguments, "-Xreturn-value-checker=check")
        assertFalse(
            "-Xreturn-value-checker=full" in testArguments,
            "production mode must not leak into compileTestKotlin",
        )
    }

    @Test
    fun returnValueCheckerDisabledForTests() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.returnValueChecker(ReturnValueCheckerMode.Check, ReturnValueCheckerMode.Disabled)
        project.evaluate()

        assertContains(project.compileArguments("compileKotlin"), "-Xreturn-value-checker=check")
        // 'disable' equals the compiler default for 'CommonCompilerArguments.returnValueChecker', so the argument
        // serializer omits it: setting the test mode to Disabled produces no -Xreturn-value-checker argument at all
        // (whereas Check/Full do).
        assertFalse(
            project.compileArguments("compileTestKotlin").any { it.startsWith("-Xreturn-value-checker") },
            "no return value checker argument should appear for the disabled test mode",
        )
    }

    @Test
    fun returnValueCheckerForTestsOnly() {
        // Only the test mode is configured; production is left unset.
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.returnValueCheckerModeForTests = ReturnValueCheckerMode.Check
        project.evaluate()

        assertFalse(
            project.compileArguments("compileKotlin").any { it.startsWith("-Xreturn-value-checker") },
            "production compilation must not get the checker when only the test mode is configured",
        )
        assertContains(project.compileArguments("compileTestKotlin"), "-Xreturn-value-checker=check")
    }

    @Test
    fun returnValueCheckerReachesAllMppTargets() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                js()
                // two native targets so that the shared 'nativeMain' (metadata) compilation is created
                linuxX64()
                linuxArm64()
                applyDefaultHierarchyTemplate()
                returnValueChecker()
            }
        }
        project.evaluate()

        // production compilations, including the shared native metadata compilation
        val productionTasks = listOf(
            "compileCommonMainKotlinMetadata",
            "compileNativeMainKotlinMetadata",
            "compileKotlinJvm",
            "compileKotlinJs",
            "compileKotlinLinuxX64",
        )
        // test compilations get the production mode by default as well
        val testTasks = listOf("compileTestKotlinJvm", "compileTestKotlinJs", "compileTestKotlinLinuxX64")
        for (task in productionTasks + testTasks) {
            assertContains(project.compileArguments(task), "-Xreturn-value-checker=check", "$task is missing return value checker")
        }
    }

    @Test
    fun returnValueCheckerSeparateTestModeReachesAllMppTargets() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                js()
                // two native targets so that the shared 'nativeMain' (metadata) compilation is created
                linuxX64()
                linuxArm64()
                applyDefaultHierarchyTemplate()
                returnValueChecker(ReturnValueCheckerMode.Full, ReturnValueCheckerMode.Check)
            }
        }
        project.evaluate()

        // Production compilations use the production mode. 'compileNativeMainKotlinMetadata' is a shared native
        // (metadata) compilation: it is production code, so it must use the production mode and not the test mode.
        val productionTasks = listOf(
            "compileCommonMainKotlinMetadata",
            "compileNativeMainKotlinMetadata",
            "compileKotlinJvm",
            "compileKotlinJs",
            "compileKotlinLinuxX64",
        )
        for (task in productionTasks) {
            assertContains(project.compileArguments(task), "-Xreturn-value-checker=full", "$task must use the production mode")
        }
        for (task in listOf("compileTestKotlinJvm", "compileTestKotlinJs", "compileTestKotlinLinuxX64")) {
            assertContains(project.compileArguments(task), "-Xreturn-value-checker=check", "$task must use the test mode")
        }
    }

    private fun Project.compileArguments(taskName: String): List<String> {
        val task = tasks.getByName(taskName) as KotlinCompilerArgumentsProducer
        return ArgumentUtils.convertArgumentsToStringList(task.createCompilerArguments(lenient))
    }
}
