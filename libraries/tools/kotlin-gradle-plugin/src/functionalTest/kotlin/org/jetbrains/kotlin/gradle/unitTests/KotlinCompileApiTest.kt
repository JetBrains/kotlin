/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.internal.KOTLIN_COMPILER_EMBEDDABLE
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.plugin.COMPILER_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinApiPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class KotlinCompileApiTest {

    @get:Rule
    val tmpDir = TemporaryFolder()

    private lateinit var project: ProjectInternal
    private lateinit var plugin: KotlinApiPlugin
    private lateinit var topLevelCompilerOptions: KotlinJvmCompilerOptions
    private lateinit var taskApi: KotlinJvmCompile
    private lateinit var taskImpl: KotlinCompile

    companion object {
        private const val TASK_NAME = "kotlinCompile"
        private const val MODULE_NAME = "customModuleName"
    }

    @Before
    fun setUpProject() {
        project = buildProject {}
        plugin = project.plugins.apply(KotlinApiPlugin::class.java)
        topLevelCompilerOptions = plugin.createCompilerJvmOptions()
        topLevelCompilerOptions.moduleName.convention(MODULE_NAME)
        plugin.registerKotlinJvmCompileTask(TASK_NAME, topLevelCompilerOptions).configure { task ->
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
        @Suppress("DEPRECATION") val task = plugin.registerKotlinJvmCompileTask("customCompileKotlin", "some-module")
        @Suppress("DEPRECATION")
        plugin.kotlinExtension.explicitApi = ExplicitApiMode.Strict
        project.evaluate()
        assertEquals(ExplicitApiMode.Strict, (task.get() as KotlinCompile).explicitApiMode.orNull)
    }

    @Test
    fun testCustomExplicitApiMode() {
        val task = plugin.registerKotlinJvmCompileTask(
            "customKotlinCompile",
            topLevelCompilerOptions,
            project.provider { ExplicitApiMode.Strict }
        )

        assertEquals((task.get() as KotlinCompile).explicitApiMode.get(), ExplicitApiMode.Strict)
    }

    @Test
    fun testTopLevelCompilerOptionsCouldBeOverriden() {
        taskApi.compilerOptions.progressiveMode.set(false)
        topLevelCompilerOptions.progressiveMode.set(true)

        assertEquals(false, taskApi.compilerOptions.progressiveMode.get())
    }

    @Test
    fun testBuiltToolsApiVersion() {
        val compilerDependency = project.configurations
            .getByName(COMPILER_CLASSPATH_CONFIGURATION_NAME)
            .incoming
            .dependencies
            .single()

        assertEquals(
            "$KOTLIN_MODULE_GROUP:$KOTLIN_COMPILER_EMBEDDABLE:${plugin.pluginVersion}",
            "${compilerDependency.group}:${compilerDependency.name}:${compilerDependency.version}"
        )
    }

    @Test
    fun testCreatingJvmExtension() {
        val jvmExtension = plugin.createKotlinJvmExtension()

        val jvmTask = plugin.registerKotlinJvmCompileTask(
            "jvmTask",
            jvmExtension.compilerOptions,
            plugin.providerFactory.provider {  jvmExtension.explicitApi }
        )

        jvmExtension.compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
        jvmExtension.compilerOptions.javaParameters.set(true)
        jvmExtension.explicitApi = ExplicitApiMode.Strict
        jvmExtension.sourceSets.register("main")

        project.evaluate()

        assertEquals(JvmTarget.JVM_21, jvmTask.get().compilerOptions.jvmTarget.get())
        assertEquals(true, jvmTask.get().compilerOptions.javaParameters.get())
        assertEquals(
            ExplicitApiMode.Strict,
            (jvmTask.get() as KotlinCompile).explicitApiMode.get()
        )
        assertNotNull(jvmExtension.sourceSets.findByName("main"))
    }

    @Test
    fun testEachUniqueCreatedJvmExtensionUnique() {
        val jvmExtension1 = plugin.createKotlinJvmExtension()
        val jvmExtension2 = plugin.createKotlinJvmExtension()

        assertNotEquals(jvmExtension1, jvmExtension2)
    }

    @Test
    fun testCreatingAndroidExtension() {
        val androidExtension = plugin.createKotlinAndroidExtension()

        val androidTask = plugin.registerKotlinJvmCompileTask(
            "jvmTask",
            androidExtension.compilerOptions,
            plugin.providerFactory.provider { androidExtension.explicitApi }
        )

        androidExtension.compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
        androidExtension.compilerOptions.javaParameters.set(true)
        androidExtension.explicitApi = ExplicitApiMode.Strict
        androidExtension.sourceSets.register("main")

        project.evaluate()

        assertEquals(JvmTarget.JVM_21, androidTask.get().compilerOptions.jvmTarget.get())
        assertEquals(true, androidTask.get().compilerOptions.javaParameters.get())
        assertEquals(
            ExplicitApiMode.Strict,
            (androidTask.get() as KotlinCompile).explicitApiMode.get()
        )
        assertNotNull(androidExtension.sourceSets.findByName("main"))
    }

    @Test
    fun testEachUniqueCreatedAndroidExtensionUnique() {
        val androidExtension1 = plugin.createKotlinAndroidExtension()
        val androidExtension2 = plugin.createKotlinAndroidExtension()

        assertNotEquals(androidExtension1, androidExtension2)
    }
}