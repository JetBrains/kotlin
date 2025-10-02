/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ArgumentType.PluginClasspath
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import org.jetbrains.kotlin.gradle.util.kotlin
import java.io.File
import kotlin.test.Test
import kotlin.test.fail

@Suppress("FunctionName")
class KotlinSubpluginApiTest {
    @Test
    fun `parcelize - is applicable to android only - old target`() {
        val project = buildProjectWithMPP {
            configureRepositoriesForTests()
            androidLibrary {}
            plugins.apply("kotlin-parcelize")
            kotlin {
                androidTarget()
                jvm()
                linuxX64()
                js()

                @OptIn(ExperimentalWasmDsl::class)
                wasmJs()
                @OptIn(ExperimentalWasmDsl::class)
                wasmWasi()
            }
        }
        project.evaluate()

        val pluginClasspathResolutionContext = CreateCompilerArgumentsContext(
            includeArgumentTypes = setOf(PluginClasspath)
        )

        val allButAndroid = project.multiplatformExtension.targets.filter { it.name != "android" }
        val parcelizeJar = "kotlin-parcelize-compiler-${project.getKotlinPluginVersion()}.jar"
        allButAndroid.flatMap { it.compilations }.forEach { compilation ->
            val compileTask = compilation.compileTaskProvider.get() as KotlinCompilerArgumentsProducer
            val args = compileTask.createCompilerArguments(pluginClasspathResolutionContext) as CommonCompilerArguments
            if (args.pluginClasspaths.orEmpty().any { File(it).name == parcelizeJar })
                fail("No kotlin-parcelize plugin should be present in args for compile task $compileTask")
        }

        val androidTarget = project.multiplatformExtension.targets.getByName("android")
        androidTarget.compilations.forEach { compilation ->
            val compileTask = compilation.compileTaskProvider.get() as KotlinCompilerArgumentsProducer
            val args = compileTask.createCompilerArguments(pluginClasspathResolutionContext) as CommonCompilerArguments
            if (args.pluginClasspaths.orEmpty().none { File(it).name == parcelizeJar })
                fail("kotlin-parcelize plugin should be present in args for compile task $compileTask")
        }
    }
}