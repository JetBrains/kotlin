/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.JavaVersion
import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import kotlin.test.*

class KotlinJvmRunTest {
    @Test
    fun `test - simple jvm target`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = project.multiplatformExtension
        assertNotNull(kotlin.jvm())
        project.configurationResult.await()

        val task = project.tasks.findByName("jvmRun") ?: fail("Missing 'jvmRun' run task")
        if (task !is JavaExec) fail("Expected $task to implement '${JavaExec::class}'")
        assertNoDiagnostics()
    }

    @Test
    fun `test - mainClass - set via DSL`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = project.multiplatformExtension
        val mainRun = assertNotNull(kotlin.jvm().mainRun.await())
        val mainRunTask = mainRun.task.get()

        assertNull(
            mainRunTask.mainClass.orNull,
            "Expected mainClass to be null: No mainClass set via DSL or property"
        )

        // Set mainClass via DSL
        mainRun.mainClass.set("Foo")
        assertEquals("Foo", mainRunTask.mainClass.orNull)
    }


    @Test
    fun `test - args`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = project.multiplatformExtension
        val task = assertNotNull(kotlin.jvm().mainRun.await()).task.get()

        /* Set properties using 'args()' */
        kotlin.jvm().mainRun {
            args("first", "second")
            args(listOf("third", "fourth"))
        }

        launch {
            assertEquals(listOf("first", "second", "third", "fourth"), task.args)
        }


        /* Set property using .setArgs */
        kotlin.jvm().mainRun {
            setArgs(listOf("1", "2"))
        }

        launch {
            assertEquals(listOf("1", "2"), task.args)
        }
    }

    @Test
    fun `test - classpath - contains main compilation output by default`() = buildProjectWithMPP().runLifecycleAwareTest {
        repositories.mavenLocal()
        repositories.mavenCentralCacheRedirector()

        val kotlin = project.multiplatformExtension
        val mainRunTask = assertNotNull(kotlin.jvm().mainRun.await()).task.get()
        val mainCompilation = kotlin.jvm().compilations.main

        configurationResult.await()

        mainCompilation.output.allOutputs.files.ifEmpty { fail("Expected some file in 'allOutputs'") }
        if (!mainRunTask.classpath.files.containsAll(mainCompilation.output.allOutputs.files)) {
            fail("Missing output from main compilation in '$mainRunTask'")
        }
    }

    @Test
    fun `test - classpath - contains main compilation runtime dependencies`() = buildProjectWithMPP().runLifecycleAwareTest {
        repositories.mavenLocal()
        repositories.mavenCentralCacheRedirector()

        val kotlin = project.multiplatformExtension

        kotlin.jvm().compilations.main.defaultSourceSet.dependencies {
            implementation(project.files("implementation.jar"))
            compileOnly(project.files("compileOnly.jar"))
            runtimeOnly(project.files("runtimeOnly.jar"))
        }


        configurationResult.await()
        val mainRunTask = assertNotNull(kotlin.jvm().mainRun.await()?.task?.get())

        if (project.file("implementation.jar") !in mainRunTask.classpath) {
            fail("Missing file from 'implementation' scope")
        }

        if (project.file("runtimeOnly.jar") !in mainRunTask.classpath) {
            fail("Missing file from 'runtimeOnly' scope")
        }

        if (project.file("compileOnly.jar") in mainRunTask.classpath) {
            fail("Unexpected file from 'compileOnly' scope")
        }
    }

    @Test
    fun `test - classpath - add file`() = buildProjectWithMPP().runLifecycleAwareTest {
        repositories.mavenLocal()
        repositories.mavenCentralCacheRedirector()
        val kotlin = project.multiplatformExtension
        kotlin.jvm().mainRun { classpath(file("custom.jar")) }

        configurationResult.await()

        val task = assertNotNull(kotlin.jvm().mainRun.await()).task.get()
        if (file("custom.jar") !in task.classpath.files) {
            fail("Missing custom.jar in classpath")
        }

        if (task.classpath.files.size <= 1) {
            fail("Expected more files in classpath than just custom.jar")
        }
    }

    @Test
    fun `test - setClasspath`() = buildProjectWithMPP().runLifecycleAwareTest {
        repositories.mavenLocal()
        repositories.mavenCentralCacheRedirector()
        val kotlin = project.multiplatformExtension
        kotlin.jvm().mainRun {
            setClasspath(project.files("a.jar", "b.jar"))
        }

        project.configurationResult.await()
        assertEquals(
            project.files("a.jar", "b.jar").files,
            kotlin.jvm().mainRun.await()?.task?.get()?.classpath?.files
        )
    }

    @Test
    fun `test - jvmRun task is already registered`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.jvm()
        tasks.register("jvmRun")
        configurationResult.await()
        checkDiagnostics("jvmRunTask-conflict")
    }

    @Test
    fun `test - jvmRun task is using kotlin configured toolchain - jvm 11`() = buildProjectWithMPP().runLifecycleAwareTest {
        Assume.assumeFalse("https://github.com/gradle/native-platform/issues/274", HostManager.hostIsMingw)
        val kotlin = multiplatformExtension
        kotlin.jvmToolchain(11)
        kotlin.jvm()
        configurationResult.await()
        assertEquals(JavaVersion.VERSION_11, assertNotNull(kotlin.jvm().mainRun.await()).task.get().javaVersion)
    }

    @Test
    fun `test - jvmRun task is using kotlin configured toolchain - jvm 17`() = buildProjectWithMPP().runLifecycleAwareTest {
        Assume.assumeFalse("https://github.com/gradle/native-platform/issues/274", HostManager.hostIsMingw)
        val kotlin = multiplatformExtension
        kotlin.jvmToolchain(17)
        kotlin.jvm()
        configurationResult.await()
        assertEquals(JavaVersion.VERSION_17, assertNotNull(kotlin.jvm().mainRun.await()).task.get().javaVersion)
    }
}

