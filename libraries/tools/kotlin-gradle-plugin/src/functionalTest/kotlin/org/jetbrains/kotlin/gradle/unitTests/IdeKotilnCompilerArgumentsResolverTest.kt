/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeCompilerArgumentsResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import kotlin.test.Test
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
}
