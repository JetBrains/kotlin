/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.plugin.KotlinApiPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinJvmFactory
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinCompileApiTest {

    @get:Rule
    val tmpDir = TemporaryFolder()

    private lateinit var project: ProjectInternal
    private lateinit var plugin: KotlinJvmFactory
    private lateinit var taskApi: KotlinJvmCompile
    private lateinit var taskImpl: KotlinCompile

    companion object {
        private const val TASK_NAME = "kotlinCompile"
    }

    @Before
    fun setUpProject() {
        project = buildProject {}
        plugin = project.plugins.apply(KotlinApiPlugin::class.java)
        plugin.registerKotlinJvmCompileTask(TASK_NAME).configure { task ->
            taskApi = task
        }
        taskImpl = project.tasks.getByName(TASK_NAME) as KotlinCompile
    }

    @Test
    fun testTaskName() {
        assertEquals(TASK_NAME, taskImpl.name)
    }

    @Test
    fun testSources() {
        val sourcePath = tmpDir.newFolder().resolve("foo.kt").also {
            it.createNewFile()
        }
        taskApi.setSource(sourcePath)
        assertEquals(setOf(sourcePath), taskImpl.sources.files)

        val sourcesDir = tmpDir.newFolder().also {
            it.resolve("a.kt").createNewFile()
            it.resolve("b.kt").createNewFile()
        }
        taskApi.setSource(sourcePath, sourcesDir)
        assertEquals(
            setOf(sourcePath, sourcesDir.resolve("a.kt"), sourcesDir.resolve("b.kt")),
            taskImpl.sources.files
        )
    }

    @Test
    fun testFriendPaths() {
        val friendPath = tmpDir.newFolder()
        taskApi.friendPaths.from(friendPath)
        assertEquals(setOf(friendPath), taskImpl.friendPaths.files)
    }

    @Test
    fun testLibraries() {
        val classpathEntries = setOf(tmpDir.newFolder(), tmpDir.newFolder())
        taskApi.libraries.from(classpathEntries)
        assertEquals(classpathEntries, taskImpl.libraries.files)
    }

    @Test
    fun testPluginClasspath() {
        val pluginDependency = tmpDir.newFile()
        plugin.addCompilerPluginDependency(project.provider { project.files(pluginDependency) })

        val anotherCompilerPlugin = tmpDir.newFile()
        taskApi.pluginClasspath.from(plugin.getCompilerPlugins(), anotherCompilerPlugin)
        assertEquals(setOf(pluginDependency, anotherCompilerPlugin), taskImpl.pluginClasspath.files)
    }

    @Suppress("DEPRECATION")
    @Test
    fun testModuleName() {
        taskApi.moduleName.set("foo")
        assertEquals("foo", taskImpl.moduleName.get())
    }

    @Test
    fun testSourceSetName() {
        taskApi.sourceSetName.set("sourceSetFoo")
        assertEquals("sourceSetFoo", taskImpl.sourceSetName.get())
    }

    @Test
    fun testTaskBuildDirectory() {
        val taskBuildDir = tmpDir.newFolder()
        taskApi.destinationDirectory.fileValue(taskBuildDir)
        assertEquals(taskBuildDir, taskImpl.destinationDirectory.get().asFile)
    }

    @Test
    fun testDestinationDirectory() {
        val destinationDir = tmpDir.newFolder()
        taskApi.destinationDirectory.fileValue(destinationDir)
        assertEquals(destinationDir, taskImpl.destinationDirectory.get().asFile)
    }

    @Test
    fun testMultiplatform() {
        taskApi.multiPlatformEnabled.set(true)
        assertEquals(true, taskImpl.multiPlatformEnabled.get())

        taskApi.multiPlatformEnabled.set(false)
        assertEquals(false, taskImpl.multiPlatformEnabled.get())
    }

    @Test
    fun testModuleDetection() {
        taskApi.useModuleDetection.set(true)
        assertEquals(true, taskImpl.useModuleDetection.get())

        taskApi.useModuleDetection.set(false)
        assertEquals(false, taskImpl.useModuleDetection.get())
    }

    @Test
    fun testTopLevelExtension() {
        plugin.kotlinExtension.explicitApi = ExplicitApiMode.Strict
        project.evaluate()
        assertEquals(ExplicitApiMode.Strict, taskImpl.explicitApiMode.orNull)
    }
}