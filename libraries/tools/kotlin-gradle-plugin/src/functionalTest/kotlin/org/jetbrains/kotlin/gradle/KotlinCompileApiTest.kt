/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.mpp.buildProject
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig
import org.jetbrains.kotlin.gradle.plugin.KotlinBaseApiPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinJvmFactory
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileOptions
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
    private lateinit var options: KotlinCompileOptions
    private lateinit var kotlinCompileTask: KotlinCompile

    companion object {
        private const val TASK_NAME = "kotlinCompile"
    }

    @Before
    fun setUpProject() {
        project = buildProject {}
        plugin = project.plugins.apply(KotlinBaseApiPlugin::class.java)
        plugin.createKotlinCompileTask(TASK_NAME) { _, kotlinCompileOptions ->
            options = kotlinCompileOptions
        }
        kotlinCompileTask = project.tasks.getByName(TASK_NAME) as KotlinCompile
    }

    @Test
    fun testTaskName() {
        assertEquals(TASK_NAME, kotlinCompileTask.name)
    }

    @Test
    fun testSources() {
        val sourcePath = tmpDir.newFolder().resolve("foo.kt").also {
            it.createNewFile()
        }
        options.source.from(sourcePath)
        assertEquals(setOf(sourcePath), kotlinCompileTask.stableSources.files)

        val sourcesDir = tmpDir.newFolder().also {
            it.resolve("a.kt").createNewFile()
            it.resolve("b.kt").createNewFile()
        }
        options.source.setFrom(sourcePath, sourcesDir)
        assertEquals(
            setOf(sourcePath, sourcesDir.resolve("a.kt"), sourcesDir.resolve("b.kt")),
            kotlinCompileTask.stableSources.files
        )
    }

    @Test
    fun testFriendPaths() {
        val friendPath = tmpDir.newFolder()
        options.friendPaths.from(friendPath)
        assertEquals(setOf(friendPath), kotlinCompileTask.friendPaths.files)
    }

    @Test
    fun testClasspath() {
        val classpathEntries = setOf(tmpDir.newFolder(), tmpDir.newFolder())
        options.classpath.from(classpathEntries)
        assertEquals(classpathEntries, kotlinCompileTask.classpath.files)
    }

    @Test
    fun testPluginClasspath() {
        val pluginDependency = tmpDir.newFile()
        plugin.addCompilerPluginDependency(project.files(pluginDependency))

        val anotherCompilerPlugin = tmpDir.newFile()
        options.pluginClasspath.from(plugin.getCompilerPlugins(), anotherCompilerPlugin)
        assertEquals(setOf(pluginDependency, anotherCompilerPlugin), kotlinCompileTask.pluginClasspath.files)
    }

    @Test
    fun testModuleName() {
        options.moduleName.set("foo")
        assertEquals("foo", kotlinCompileTask.moduleName.get())
    }

    @Test
    fun testSourceSetName() {
        options.sourceSetName.set("sourceSetFoo")
        assertEquals("sourceSetFoo", kotlinCompileTask.sourceSetName.get())
    }

    @Test
    fun testTaskBuildDirectory() {
        val taskBuildDir = tmpDir.newFolder()
        options.taskBuildDirectory.fileValue(taskBuildDir)
        assertEquals(taskBuildDir, kotlinCompileTask.taskBuildDirectory.get().asFile)
    }

    @Test
    fun testDestinationDirectory() {
        val destinationDir = tmpDir.newFolder()
        options.destinationDir.fileValue(destinationDir)
        assertEquals(destinationDir, kotlinCompileTask.destinationDirectory.get().asFile)
    }

    @Test
    fun testMultiplatform() {
        options.multiPlatformEnabled.set(true)
        assertEquals(true, kotlinCompileTask.multiPlatformEnabled.get())

        options.multiPlatformEnabled.set(false)
        assertEquals(false, kotlinCompileTask.multiPlatformEnabled.get())
    }

    @Test
    fun testModuleDetection() {
        options.useModuleDetection.set(true)
        assertEquals(true, kotlinCompileTask.useModuleDetection.get())

        options.useModuleDetection.set(false)
        assertEquals(false, kotlinCompileTask.useModuleDetection.get())
    }


    @Test
    fun testParentKotlinOptions() {
        val parentOptions = plugin.createKotlinJvmDsl()
        parentOptions.moduleName = "foo"
        parentOptions.javaParameters = true
        parentOptions.languageVersion = "lang_version"
        options.parentKotlinOptions.set(parentOptions)

        kotlinCompileTask.parentKotlinOptionsImpl.get().let {
            assertEquals(parentOptions.moduleName, it.moduleName)
            assertEquals(parentOptions.javaParameters, it.javaParameters)
            assertEquals(parentOptions.languageVersion, it.languageVersion)
        }
    }

    @Test
    fun testCompilerPluginOptions() {
        val pluginOptions = CompilerPluginConfig()
        pluginOptions.addPluginArgument("foo", SubpluginOption("bar", "bar"))
        pluginOptions.addPluginArgument("foo", SubpluginOption("bar1", "bar1"))
        options.additionalPluginOptions.add(pluginOptions)

        assertEquals(pluginOptions.allOptions(), kotlinCompileTask.pluginOptions.get().single().allOptions())
    }

    @Test
    fun testTopLevelExtension() {
        plugin.kotlinExtension.explicitApi = ExplicitApiMode.Strict
        project.evaluate()
        assertTrue(ExplicitApiMode.Strict.toCompilerArg() in kotlinCompileTask.kotlinOptions.freeCompilerArgs)
    }

    @Test
    fun testTopLevelExtensionFromConfigHasPrecedence() {
        options.explicitApiMode.set(ExplicitApiMode.Disabled)
        plugin.kotlinExtension.explicitApi = ExplicitApiMode.Strict
        project.evaluate()
        assertTrue(ExplicitApiMode.Disabled.toCompilerArg() in kotlinCompileTask.kotlinOptions.freeCompilerArgs)
    }
}