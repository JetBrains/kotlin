/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeCompilerArgumentsResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.main
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class IdeCompilerArgumentsResolverTest {

    @Test
    fun `test - compilation and compile task resolve same arguments`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.linuxX64()
        kotlin.linuxArm64()
        kotlin.jvm()
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        kotlin.targets.flatMap { it.compilations }.forEach { compilation ->
            val compileTask = compilation.compileTaskProvider.get()
            val byCompilation = project.kotlinIdeCompilerArgumentsResolver.resolveCompilerArguments(compilation)
            val byCompileTask = project.kotlinIdeCompilerArgumentsResolver.resolveCompilerArguments(compileTask)

            if (byCompilation.isNullOrEmpty()) fail("Failed resolving arguments for compilation $compilation")
            if (byCompileTask.isNullOrEmpty()) fail("Failed resolving arguments for compileTask: ${compileTask.path}")

            if (byCompilation != byCompileTask) {
                fail("Arguments resolved from compileTask do not match arguments resolved for compilation ($compilation)")
            }
        }
    }

    @Test
    fun `test - resolved arguments can be correctly parsed`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.linuxX64()
        kotlin.linuxArm64()
        kotlin.jvm()
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        kotlin.targets.flatMap { it.compilations }.forEach { compilation ->
            val argumentsList = project.kotlinIdeCompilerArgumentsResolver.resolveCompilerArguments(compilation)
                ?: fail("Missing arguments for $compilation")

            val parsedArguments = when (compilation.platformType) {
                KotlinPlatformType.common -> if (compilation is KotlinSharedNativeCompilation)
                    parseCommandLineArguments<K2NativeCompilerArguments>(argumentsList)
                else parseCommandLineArguments<K2MetadataCompilerArguments>(argumentsList)
                KotlinPlatformType.jvm, KotlinPlatformType.androidJvm -> parseCommandLineArguments<K2JVMCompilerArguments>(argumentsList)
                KotlinPlatformType.js, KotlinPlatformType.wasm -> parseCommandLineArguments<K2JSCompilerArguments>(argumentsList)
                KotlinPlatformType.native -> parseCommandLineArguments<K2NativeCompilerArguments>(argumentsList)
            }

            val parsedArgumentsList = parsedArguments.toArgumentStrings(shortArgumentKeys = true, compactArgumentValues = false)
            if (argumentsList != parsedArgumentsList) {
                /* Mismatch might indicate that something was not correctly parsed */
                fail("$compilation: Expected parsed arguments list to match origin arguments list")
            }
        }
    }

    @Test
    fun `test - language and api version follow compilerVersion`() {
        assertEquals(mapOf("languageVersion" to "2.3", "apiVersion" to "2.3"), jvmMainLevels { compilerVersion.set("2.3.0") })
    }

    @Test
    fun `test - explicit api version wins over compilerVersion`() {
        val levels = jvmMainLevels {
            compilerVersion.set("2.4.0")
            compilerOptions.apiVersion.set(KotlinVersion.KOTLIN_2_3)
        }
        assertEquals(mapOf("languageVersion" to "2.4", "apiVersion" to "2.3"), levels)
    }

    @Test
    fun `test - explicit language version wins and caps api version`() {
        val levels = jvmMainLevels {
            compilerVersion.set("2.4.0")
            compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_2_3)
        }
        assertEquals(mapOf("languageVersion" to "2.3", "apiVersion" to "2.3"), levels)
    }

    @Test
    fun `test - language and api version stay unset without compilerVersion`() {
        assertEquals(mapOf("languageVersion" to null, "apiVersion" to null), jvmMainLevels { })
    }

    @Test
    fun `test - multiplatform compilations follow compilerVersion except native ones`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                js()
                linuxX64()
                compilerVersion.set("2.4.0")
            }
        }.evaluate()

        val languageVersions = project.multiplatformExtension.targets.associate { target ->
            target.name to target.compilations.main.compileTaskProvider.get().compilerOptions.languageVersion.orNull
        }
        assertEquals(
            mapOf(
                "metadata" to KotlinVersion.KOTLIN_2_4,
                "jvm" to KotlinVersion.KOTLIN_2_4,
                "js" to KotlinVersion.KOTLIN_2_4,
                // Native targets are always compiled with the compiler of the plugin's version
                "linuxX64" to null,
            ),
            languageVersions,
        )
    }

    private fun jvmMainLevels(configure: KotlinJvmProjectExtension.() -> Unit): Map<String, String?> {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.configure()
        project.evaluate()
        return project.levels(project.kotlinJvmExtension.target.compilations.main)
    }

    private fun Project.levels(compilation: KotlinCompilation<*>): Map<String, String?> {
        val argumentsList = kotlinIdeCompilerArgumentsResolver.resolveCompilerArguments(compilation)
            ?: fail("Missing arguments for $compilation")
        val arguments = parseCommandLineArguments<K2JVMCompilerArguments>(argumentsList)
        return mapOf("languageVersion" to arguments.languageVersion, "apiVersion" to arguments.apiVersion)
    }
}
