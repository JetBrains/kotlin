/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinApiPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinJvmFactory
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinCompileToolTest {

    @get:Rule
    val tmpDir = TemporaryFolder()

    private lateinit var project: Project
    private lateinit var kotlinJvmFactory: KotlinJvmFactory

    @Before
    fun setUpProject() {
        project = buildProject {}
        kotlinJvmFactory = project.plugins.apply(KotlinApiPlugin::class.java)
    }

    @Test
    fun callingSourceShouldAddSources() {
        val task = kotlinJvmFactory.registerKotlinJvmCompileTask(
            taskName = "test",
            compilerOptions = kotlinJvmFactory.createCompilerJvmOptions().apply { moduleName.set("test") }
        ).get()
        val kotlinSourceFiles = testKotlinSourceFiles()

        task.source(kotlinSourceFiles[1], kotlinSourceFiles[0])
        task.source(kotlinSourceFiles[2])

        assertEquals(
            kotlinSourceFiles,
            task.sources.files.toList().sortedBy { it.name }, // insertion order isn't important
        )
    }

    @Test
    fun callingSetSourceShouldReplaceSources() {
        val task = kotlinJvmFactory.registerKotlinJvmCompileTask(
            taskName = "test",
            compilerOptions = kotlinJvmFactory.createCompilerJvmOptions().apply { moduleName.set("test") }).get()
        val kotlinSourceFiles = testKotlinSourceFiles()

        task.source(kotlinSourceFiles[0], kotlinSourceFiles[1])
        task.setSource(kotlinSourceFiles[2])

        assertEquals(
            listOf(kotlinSourceFiles[2]),
            task.sources.files.toList(),
        )
    }

    @Test
    fun callingSourceShouldAddJavaSources() {
        val task = kotlinJvmFactory.registerKotlinJvmCompileTask(
            taskName = "test",
            compilerOptions = kotlinJvmFactory.createCompilerJvmOptions().apply { moduleName.set("test") }
        ).get() as KotlinCompile
        val javaSourceFiles = testJavaSourceFiles()

        task.source(javaSourceFiles[1], javaSourceFiles[0])
        task.source(javaSourceFiles[2])

        assertEquals(
            javaSourceFiles,
            task.javaSources.files.toList().sortedBy { it.name }, // insertion order isn't important
        )
    }

    @Test
    fun callingSetSourceShouldReplaceJavaSources() {
        val task = kotlinJvmFactory.registerKotlinJvmCompileTask(
            taskName = "test",
            compilerOptions = kotlinJvmFactory.createCompilerJvmOptions().apply { moduleName.set("test") }
        ).get() as KotlinCompile
        val javaSourceFiles = testJavaSourceFiles()

        task.source(javaSourceFiles[0], javaSourceFiles[1])
        task.setSource(javaSourceFiles[2])

        assertEquals(
            listOf(javaSourceFiles[2]),
            task.javaSources.files.toList(),
        )
    }

    @Test
    fun callingSourceShouldAddScriptingSources() {
        val task = kotlinJvmFactory.registerKotlinJvmCompileTask(
            taskName = "test",
            compilerOptions = kotlinJvmFactory.createCompilerJvmOptions().apply { moduleName.set("test") }
        ).get() as KotlinCompile
        task.scriptExtensions.set(setOf("kts"))
        val scriptingSourceFiles = testScriptingSourceFiles()

        task.source(scriptingSourceFiles[1], scriptingSourceFiles[0])
        task.source(scriptingSourceFiles[2])

        assertEquals(
            scriptingSourceFiles,
            task.scriptSources.files.toList().sortedBy { it.name }, // insertion order isn't important
        )
    }

    @Test
    fun callingSetSourceShouldReplaceScriptingSources() {
        val task = kotlinJvmFactory.registerKotlinJvmCompileTask(
            taskName = "test",
            compilerOptions = kotlinJvmFactory.createCompilerJvmOptions().apply { moduleName.set("test") }
        ).get() as KotlinCompile
        task.scriptExtensions.set(setOf("kts"))
        val scriptingSourceFiles = testScriptingSourceFiles()

        task.source(scriptingSourceFiles[0], scriptingSourceFiles[1])
        task.setSource(scriptingSourceFiles[2])

        assertEquals(
            listOf(scriptingSourceFiles[2]),
            task.scriptSources.files.toList(),
        )
    }

    private fun testKotlinSourceFiles(): List<File> {
        val kotlinDir = tmpDir.newFolder("kotlin")
        return listOf(
            kotlinDir.resolveCreating("a.kt"),
            kotlinDir.resolveCreating("b.kt"),
            kotlinDir.resolveCreating("c.kt"),
        )
    }

    private fun testJavaSourceFiles(): List<File> {
        val javaDir = tmpDir.newFolder("java")
        return listOf(
            javaDir.resolveCreating("A.java"),
            javaDir.resolveCreating("B.java"),
            javaDir.resolveCreating("C.java"),
        )
    }

    private fun testScriptingSourceFiles(): List<File> {
        val scriptsDir = tmpDir.newFolder("scripts")
        return listOf(
            scriptsDir.resolveCreating("a.kts"),
            scriptsDir.resolveCreating("b.kts"),
            scriptsDir.resolveCreating("c.kts"),
        )
    }

    private fun File.resolveCreating(fileName: String) = resolve(fileName).also {
        it.createNewFile()
    }
}