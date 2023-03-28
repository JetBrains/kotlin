/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.default
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.main
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class KotlinNativeCompileArgumentsTest {
    @Test
    fun `test - simple project - old buildCompilerArgs and new CompilerArgumentsProducer - return same arguments`() {
        val project = buildProjectWithMPP()
        project.repositories.mavenLocal()

        val kotlin = project.multiplatformExtension

        kotlin.linuxArm64()
        val linuxX64Target = kotlin.linuxX64()

        project.evaluate()

        /* Check linuxX64 main compilation as 'native platform compilation' */
        run {
            val linuxX64MainCompilation = linuxX64Target.compilations.main
            val linuxX64MainCompileTask = linuxX64MainCompilation.compileTaskProvider.get()
            `assert buildCompilerArgs and createCompilerArgs are equal`(linuxX64MainCompileTask)
        }

        /* Check commonMain compilation as 'shared native compilation' */
        run {
            val commonMainCompilation = kotlin.metadata().compilations.getByName("commonMain")
            val commonMainCompileTask = commonMainCompilation.compileTaskProvider.get() as KotlinNativeCompile
            `assert buildCompilerArgs and createCompilerArgs are equal`(commonMainCompileTask)
        }
    }


    private fun `assert buildCompilerArgs and createCompilerArgs are equal`(compile: KotlinNativeCompile) {
        val argumentsFromCompilerArgumentsProducer = compile.createCompilerArguments(lenient)
        val argumentsFromBuildCompilerArgs = K2NativeCompilerArguments().apply {
            parseCommandLineArguments(compile.buildCompilerArgs(true), this)
        }

        assertEquals(
            ArgumentUtils.convertArgumentsToStringList(argumentsFromBuildCompilerArgs),
            ArgumentUtils.convertArgumentsToStringList(argumentsFromCompilerArgumentsProducer)
        )
    }

    @Test
    fun `test - simple project - failing dependency - lenient`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        val linuxX64Target = kotlin.linuxX64()
        kotlin.sourceSets.getByName("commonMain").dependencies { implementation("not-a:dependency:1.0.0") }
        project.evaluate()

        val commonMainCompileTask = linuxX64Target.compilations.main.compileTaskProvider.get()
        assertNull(commonMainCompileTask.createCompilerArguments(lenient).libraries)
        assertFails { commonMainCompileTask.createCompilerArguments(default) }
    }

    @Test
    fun `test - opt in`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        val linuxX64Target = kotlin.linuxX64()
        linuxX64Target.compilations.all {
            it.compilerOptions.options.apply {
                optIn.add("my.OptIn")
                optIn.add("my.other.OptIn")
            }
        }

        project.evaluate()

        val arguments = linuxX64Target.compilations.main.compileTaskProvider.get().createCompilerArguments(lenient)
        assertEquals(
            listOf("my.OptIn", "my.other.OptIn"), arguments.optIn?.toList()
        )
    }
}