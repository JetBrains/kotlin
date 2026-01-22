/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask
import org.jetbrains.kotlin.gradle.plugin.KotlinApiPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinJvmFactory
import org.jetbrains.kotlin.gradle.tasks.Kapt
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import kotlin.test.Test
import kotlin.test.assertEquals
import java.io.File

class KaptApiTest {

    @field:TempDir
    lateinit var tmpDir: File

    private lateinit var project: ProjectInternal
    private lateinit var plugin: KotlinJvmFactory

    companion object {
        private const val TASK_NAME = "kapt"
        private const val GENERATE_STUBS = "kaptGenerateStubs"
    }

    @BeforeEach
    fun setUpProject() {
        project = buildProject {}
        plugin = project.plugins.apply(KotlinApiPlugin::class.java)
        project.configurations.create(Kapt3GradleSubplugin.KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME)
    }

    @Test
    fun testTaskName() {
        val task = configureKapt {}
        assertEquals(TASK_NAME, task.name)
    }

    @Test
    fun testSources() {
        val sourcePath = tmpDir.resolve("source").also { it.mkdirs() }.resolve("foo.kt").also {
            it.createNewFile()
        }
        val task = configureKapt {
            source.from(sourcePath)
        }
        assertEquals(setOf(sourcePath), task.source.files)

        val sourcesDir = tmpDir.resolve("sourcesDir").also { it.mkdirs() }.also {
            it.resolve("a.kt").createNewFile()
            it.resolve("b.kt").createNewFile()
        }
        task.source.from(sourcesDir)
        assertEquals(setOf(sourcePath, sourcesDir), task.source.files)
    }

    @Test
    fun testKaptClasspath() {
        val kaptClasspath = tmpDir.resolve("kaptClasspath").also { it.mkdirs() }
        val externalClasspath = tmpDir.resolve("externalClasspath").also { it.mkdirs() }
        val configurationNames = listOf("a", "b", "c")
        val task = configureKapt {
            this.kaptClasspath.from(kaptClasspath)
            kaptExternalClasspath.from(externalClasspath)
            kaptClasspathConfigurationNames.set(configurationNames)
        }

        assertEquals(setOf(kaptClasspath), task.kaptClasspath.files)
        assertEquals(setOf(externalClasspath), task.kaptExternalClasspath.files)
        assertEquals(configurationNames, task.kaptClasspathConfigurationNames.get())
    }

    @Test
    fun testKaptOutputs() {
        val incAptCache = tmpDir.resolve("incAptCache").also { it.mkdirs() }
        val classesDir = tmpDir.resolve("classesDir").also { it.mkdirs() }
        val destinationDir = tmpDir.resolve("destinationDir").also { it.mkdirs() }
        val generatedKotlinSources = tmpDir.resolve("generatedKotlinSources").also { it.mkdirs() }

        val task = configureKapt {
            this.incAptCache.fileValue(incAptCache)
            this.classesDir.fileValue(classesDir)
            this.destinationDir.fileValue(destinationDir)
            kotlinSourcesDestinationDir.fileValue(generatedKotlinSources)
        }

        assertEquals(incAptCache, task.incAptCache.get().asFile)
        assertEquals(classesDir, task.classesDir.get().asFile)
        assertEquals(destinationDir, task.destinationDir.get().asFile)
        assertEquals(generatedKotlinSources, task.kotlinSourcesDestinationDir.get().asFile)
    }

    @Test
    fun testClasspath() {
        val compiledSources = tmpDir.resolve("compiledSources").also { it.mkdirs() }
        val classpath = tmpDir.resolve("classpath").also { it.mkdirs() }
        val includeCompileClasspath = false

        val task = configureKapt {
            this.compiledSources.from(compiledSources)
            this.classpath.from(classpath)
            this.includeCompileClasspath.set(includeCompileClasspath)
        }

        assertEquals(setOf(compiledSources), task.compiledSources.files)
        assertEquals(setOf(classpath), task.classpath.files)
        assertEquals(includeCompileClasspath, task.includeCompileClasspath.get())
    }

    @Test
    fun testOptions() {
        val sourceCompatibility = "11"
        val addJdkClassesToMissingClasspathSnapshot = true

        val task = configureKapt {
            defaultJavaSourceCompatibility.set(sourceCompatibility)
            addJdkClassesToClasspath.set(addJdkClassesToMissingClasspathSnapshot)
        }

        assertEquals(sourceCompatibility, task.defaultJavaSourceCompatibility.get())
        assertEquals(addJdkClassesToMissingClasspathSnapshot, task.addJdkClassesToClasspath.get())
    }

    @Test
    fun testKaptExtension() {
        plugin.kaptExtension.useBuildCache = false
        plugin.kaptExtension.includeCompileClasspath = true

        val task = configureKapt {}
        assertEquals(false, task.useBuildCache)
        assertEquals(true, task.includeCompileClasspath.get())
    }

    @Test
    fun testGenerateStubs() {
        val task = plugin.registerKaptGenerateStubsTask(
            GENERATE_STUBS,
            plugin.registerKotlinJvmCompileTask("customCompileKotlin", plugin.createCompilerJvmOptions()),
            plugin.kaptExtension,
        ).get()
        assertEquals(GENERATE_STUBS, task.name)
    }

    @Test
    fun testGenerateStubsOptions() {
        val stubsDir = tmpDir.resolve("stubsDir").also { it.mkdirs() }
        val kaptClasspath = setOf(tmpDir.resolve("kaptClasspath2").also { it.mkdirs() })
        val task = plugin.registerKaptGenerateStubsTask(
            GENERATE_STUBS,
            plugin.registerKotlinJvmCompileTask("customCompileKotlin", plugin.createCompilerJvmOptions()),
            plugin.kaptExtension,
        ).let { provider ->
            provider.configure {
                it.stubsDir.fileValue(stubsDir)
                it.kaptClasspath.from(kaptClasspath)
            }
            provider.get()
        }
        assertEquals(stubsDir, task.stubsDir.get().asFile)
        assertEquals(kaptClasspath, task.kaptClasspath.files)
    }


    @Test
    fun testGenerateStubsTaskHasCompilerOptionsFromCompileTask() {
        val customCompileTask = plugin.registerKotlinJvmCompileTask(
            "customCompileKotlin",
            plugin.createCompilerJvmOptions(),
        )
        customCompileTask.configure {
            it.compilerOptions.progressiveMode.set(true)
            it.compilerOptions.freeCompilerArgs.add("-Xdump-declarations-to=foo")
            it.compilerOptions.moduleName.set("foo")
            it.compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
        }

        val kaptGenerateStubsTask = plugin.registerKaptGenerateStubsTask(
            "kaptGenerateStubs",
            customCompileTask,
            plugin.kaptExtension
        )

        assertEquals(true, kaptGenerateStubsTask.get().compilerOptions.progressiveMode.get())
        assertEquals(JvmTarget.JVM_21, kaptGenerateStubsTask.get().compilerOptions.jvmTarget.get())
        assertEquals(listOf("-Xdump-declarations-to=foo"), kaptGenerateStubsTask.get().compilerOptions.freeCompilerArgs.get())
        assertEquals("foo", kaptGenerateStubsTask.get().compilerOptions.moduleName.get())
    }

    private fun configureKapt(configAction: Kapt.() -> Unit): KaptWithoutKotlincTask {
        val provider = plugin.registerKaptTask(TASK_NAME, plugin.kaptExtension)
        provider.configure(configAction)
        return provider.get() as KaptWithoutKotlincTask
    }
}
