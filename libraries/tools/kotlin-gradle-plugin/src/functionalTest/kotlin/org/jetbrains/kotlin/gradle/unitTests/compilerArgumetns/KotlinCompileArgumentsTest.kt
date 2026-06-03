/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ArgumentType.PluginClasspath
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ArgumentType.Primitive
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.assertNotNull
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.main
import kotlin.reflect.jvm.javaField
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

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
        val argumentsFromKotlinCompilerArgumentsAware = mainCompilationTask.createCompilerArguments(
            CreateCompilerArgumentsContext(
                includeArgumentTypes = setOf(Primitive, PluginClasspath),
                isLenient = true
            )
        )

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
        val jvmTargetArgument = K2JVMCompilerArguments::jvmTarget.javaField!!.getAnnotation(Argument::class.java)!!.value
        if (jvmTargetArgument !in argumentsString) fail("Missing '$jvmTargetArgument' in argument list")
        val indexOfJvmTargetArgument = argumentsString.indexOf(jvmTargetArgument)
        val jvmTargetTargetArgumentValue = argumentsString.getOrNull(indexOfJvmTargetArgument + 1)
        assertEquals(JvmTarget.fromTarget(JavaVersion.current().toString()).target, jvmTargetTargetArgumentValue)

        val parsedArguments = K2JVMCompilerArguments().apply { parseCommandLineArguments(argumentsString, this) }
        assertNotNull(parsedArguments.jvmTarget)
        assertEquals(JvmTarget.fromTarget(JavaVersion.current().toString()).target, parsedArguments.jvmTarget)

    }

    @Test
    fun `test - multiplatform - with K2`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()

        val jvmMainCompilation = kotlin.jvm().compilations.getByName("main")
        @Suppress("DEPRECATION")
        jvmMainCompilation.compilerOptions.options.languageVersion.set(KotlinVersion.KOTLIN_2_0)

        project.evaluate()

        val jvmMainCompileTask = jvmMainCompilation.compileTaskProvider.get() as KotlinCompile
        val arguments = jvmMainCompileTask.createCompilerArguments(lenient)

        assertEquals(
            setOf("commonMain", "jvmMain"),
            arguments.assertNotNull(CommonCompilerArguments::fragments).toSet()
        )
    }

    @Test
    fun `test - multiplatform - with K2 - source filter on compile task is respected`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        val compilation = kotlin.jvm().compilations.main
        @Suppress("DEPRECATION")
        compilation.compilerOptions.options.languageVersion.set(KotlinVersion.KOTLIN_2_0)
        val compileTask = compilation.compileTaskProvider.get() as KotlinCompile

        /*
        Create Source Files
         */
        val aKt = project.file("src/jvmMain/kotlin/A.kt")
        val bKt = project.file("src/jvmMain/kotlin/B.kt")
        val cTxt = project.file("src/jvmMain/kotlin/C.txt")

        listOf(aKt, bKt, cTxt).forEach { file ->
            file.parentFile.mkdirs()
            file.writeText("Stub")
        }

        /* Expect cTxt being filtered by default by the compile task */
        assertEquals(
            setOf(
                "jvmMain:${aKt.absolutePath}",
                "jvmMain:${bKt.absolutePath}",
            ),
            compileTask.createCompilerArguments(lenient).fragmentSources.orEmpty().toSet()
        )

        /* Explicitly include the txt file */
        compileTask.include("**.txt")
        assertEquals(
            setOf(
                "jvmMain:${aKt.absolutePath}",
                "jvmMain:${bKt.absolutePath}",
                "jvmMain:${cTxt.absolutePath}",
            ),
            compileTask.createCompilerArguments(lenient).fragmentSources.orEmpty().toSet()
        )

        /* Exclude B.kt and C.txt explicitly */
        compileTask.exclude { it.file in setOf(bKt, cTxt) }
        assertEquals(
            setOf("jvmMain:${aKt.absolutePath}"),
            compileTask.createCompilerArguments(lenient).fragmentSources.orEmpty().toSet()
        )
    }

    private fun jvmCompileTask(configure: KotlinCompile.() -> Unit): KotlinCompile {
        val project = buildProjectWithJvm()
        project.evaluate()
        return (project.kotlinJvmExtension.target.compilations.main.compileTaskProvider.get() as KotlinCompile)
            .apply(configure)
    }

    @Test
    fun `test - jvm-default - free arg is not overridden without the kotlin-dsl plugin`() {
        val task = jvmCompileTask {
            // 'kotlinDslPluginIsPresent' stays at its 'false' convention
            compilerOptions.freeCompilerArgs.add("-Xjvm-default=disable")
        }

        val arguments = task.createCompilerArguments(lenient)

        assertNull(arguments.jvmDefaultStable)
        assertContains(arguments.freeArgs, "-Xjvm-default=disable")
    }

    @Test
    fun `test - jvm-default - free arg is overridden to the stable argument with the kotlin-dsl plugin`() {
        val task = jvmCompileTask {
            kotlinDslPluginIsPresent.set(true)
            compilerOptions.freeCompilerArgs.add("-Xjvm-default=all")
        }

        val arguments = task.createCompilerArguments(lenient)

        assertEquals(JvmDefaultMode.NO_COMPATIBILITY.compilerArgument, arguments.jvmDefaultStable)
        assertFalse("-Xjvm-default=all" in arguments.freeArgs, "the legacy free arg must be dropped after the override")
    }

    @Test
    fun `test - jvm-default - explicitly configured stable argument is kept with the kotlin-dsl plugin`() {
        val task = jvmCompileTask {
            kotlinDslPluginIsPresent.set(true)
            compilerOptions.jvmDefault.set(JvmDefaultMode.ENABLE)
            // mimics the '-Xjvm-default=all' free arg injected by the 'kotlin-dsl' plugin
            compilerOptions.freeCompilerArgs.add("-Xjvm-default=all")
        }

        val arguments = task.createCompilerArguments(lenient)

        // the explicit stable argument wins; the legacy free arg is dropped without overriding it
        assertEquals(JvmDefaultMode.ENABLE.compilerArgument, arguments.jvmDefaultStable)
        assertFalse("-Xjvm-default=all" in arguments.freeArgs, "the legacy free arg must be dropped")
    }
}
