/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.assertNotNull
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.reflect.full.findAnnotation
import kotlin.test.*


class KotlinCompileArgumentsTest {

    @Test
    fun `test - simple project - compare CompilerArgumentsAware with KotlinCompilerArgumentsAware implementations`() {
        val project = buildProjectWithJvm()

        project.repositories {
            mavenLocal()
            mavenCentralCacheRedirector()
        }

        val kotlin = project.kotlinJvmExtension
        project.evaluate()

        val mainCompilation = kotlin.target.compilations.getByName("main")
        val mainCompilationTask = mainCompilation.compileTaskProvider.get() as KotlinCompile
        val argumentsFromKotlinCompilerArgumentsAware = mainCompilationTask.createCompilerArguments(lenient)

        @Suppress("DEPRECATION_ERROR")
        assertEquals(
            mainCompilationTask.serializedCompilerArgumentsIgnoreClasspathIssues,
            ArgumentUtils.convertArgumentsToStringList(argumentsFromKotlinCompilerArgumentsAware)
        )
    }

    /**
     * The jvmTargets default argument value is up for change over time.
     * The argument shall always be explicitly set!
     */
    @Test
    fun `test - simple project - jvmTarget is explicit - and uses correct default`() {
        val project = buildProjectWithJvm()
        val kotlin = project.kotlinJvmExtension
        project.evaluate()

        val mainCompilation = kotlin.target.compilations.getByName("main")
        val mainCompilationTask = mainCompilation.compileTaskProvider.get() as KotlinCompile

        val arguments = mainCompilationTask.createCompilerArguments(lenient)

        val argumentsString = ArgumentUtils.convertArgumentsToStringList(arguments)
        val jvmTargetArgument = K2JVMCompilerArguments::jvmTarget.findAnnotation<Argument>()!!.value
        if (jvmTargetArgument !in argumentsString) fail("Missing '$jvmTargetArgument' in argument list")
        val indexOfJvmTargetArgument = argumentsString.indexOf(jvmTargetArgument)
        val jvmTargetTargetArgumentValue = argumentsString.getOrNull(indexOfJvmTargetArgument + 1)
        assertEquals(JvmTarget.DEFAULT.description, jvmTargetTargetArgumentValue)

        val parsedArguments = K2JVMCompilerArguments().apply { parseCommandLineArguments(argumentsString, this) }
        assertNotNull(parsedArguments.jvmTarget)
        assertEquals(JvmTarget.DEFAULT.description, parsedArguments.jvmTarget)

    }

    @Test
    fun `test - multiplatform - with K2`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()

        val jvmMainCompilation = kotlin.jvm().compilations.getByName("main")
        jvmMainCompilation.compilerOptions.options.languageVersion.set(KotlinVersion.KOTLIN_2_0)

        project.evaluate()

        val jvmMainCompileTask = jvmMainCompilation.compileTaskProvider.get() as KotlinCompile
        val arguments = jvmMainCompileTask.createCompilerArguments(lenient)

        arguments.assertNotNull(CommonCompilerArguments::fragments).let { fragments ->
            assertEquals(setOf("commonMain", "jvmMain"), fragments.toSet())
        }
    }
}