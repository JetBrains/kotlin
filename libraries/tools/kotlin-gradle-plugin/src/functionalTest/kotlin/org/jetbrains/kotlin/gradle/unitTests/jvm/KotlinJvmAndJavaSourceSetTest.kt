/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.jvm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.util.assertNoCircularTaskDependencies
import org.jetbrains.kotlin.gradle.util.assertTaskDependenciesEquals
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.configureDefaults
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.androidExtension
import org.jetbrains.kotlin.gradle.utils.javaSourceSets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotlinJvmAndJavaSourceSetTest {
    private val baseJvmProject = buildProjectWithMPP {
        kotlin {
            jvm()
        }
    }

    @Test
    fun shouldCreateJavaSourceSetForMainCompilation() {
        baseJvmProject.evaluate()

        val jvmMainCompilation = baseJvmProject.multiplatformExtension.jvmMainCompilation
        val mainSourceSet = baseJvmProject.javaSourceSets.findByName(
            jvmMainCompilation.defaultSourceSet.name
        )
        assertNotNull(mainSourceSet)
        val srcDirs = mainSourceSet.java.srcDirs
        assertTrue(
            srcDirs.size == 1,
            "Main Java source set contains more than one SourceDirectorySet: $srcDirs"
        )
        assertTrue(
            srcDirs.single().endsWith("src/jvmMain/java"),
            "Main java source set does not use 'src/jvmMain/java' as SourceDirectorySet"
        )

        assertNotEquals(
            jvmMainCompilation.compileDependencyConfigurationName,
            mainSourceSet.compileClasspathConfigurationName,
        )
        assertTrue(
            baseJvmProject.configurations
                .getByName(mainSourceSet.compileClasspathConfigurationName)
                .extendsFrom
                .containsAll(
                    listOf(
                        baseJvmProject.configurations.getByName(jvmMainCompilation.apiConfigurationName),
                        baseJvmProject.configurations.getByName(jvmMainCompilation.implementationConfigurationName),
                        baseJvmProject.configurations.getByName(jvmMainCompilation.compileOnlyConfigurationName),
                    )
                )
        )
    }

    @Test
    fun mainJavaSourceSetOutputShouldBeIncludedInFinalArtifact() {
        baseJvmProject.evaluate()

        val jvmMainCompilation = baseJvmProject.multiplatformExtension.jvmMainCompilation
        assertTrue(
            jvmMainCompilation.output.classesDirs.files.containsAll(
                baseJvmProject.jvmSourceSet(jvmMainCompilation).output.classesDirs.files
            )
        )
    }

    @Test
    fun mainJavaCompileTaskIsIncludedAsClassesTaskDependency() {
        baseJvmProject.evaluate()

        val jvmMainCompilation = baseJvmProject.multiplatformExtension.jvmMainCompilation
        val jvmMainClassesTask = baseJvmProject.tasks.getByName(jvmMainCompilation.compileAllTaskName)
        val javaProcessResourcesTask = baseJvmProject.tasks.getByName(jvmMainCompilation.defaultJavaSourceSet.processResourcesTaskName)

        jvmMainClassesTask.assertNoCircularTaskDependencies()
        jvmMainClassesTask.assertTaskDependenciesEquals(
            setOf(
                jvmMainCompilation.compileTaskProvider.get(),
                jvmMainCompilation.defaultCompileJavaProvider.get(),
                baseJvmProject.tasks.getByName(jvmMainCompilation.processResourcesTaskName),
                javaProcessResourcesTask
            )
        )
        assertFalse(javaProcessResourcesTask.enabled, "Java process resources task '${javaProcessResourcesTask.name} should be disabled")
    }

    @Test
    fun shouldCreateJavaSourceSetForTestCompilation() {
        baseJvmProject.evaluate()

        val testSourceSet = baseJvmProject.javaSourceSets.findByName(
            baseJvmProject.multiplatformExtension.jvmTestCompilation.defaultSourceSet.name
        )
        assertNotNull(testSourceSet)
        val srcDirs = testSourceSet.java.srcDirs
        assertTrue(
            srcDirs.size == 1,
            "Test Java source set contains more than one SourceDirectorySet: $srcDirs"
        )
        assertTrue(
            srcDirs.single().endsWith("src/jvmTest/java"),
            "Test java source set does not use 'src/jvmTest/java' as SourceDirectorySet"
        )

        assertEquals(
            baseJvmProject.multiplatformExtension.jvmTestCompilation.compileDependencyConfigurationName,
            testSourceSet.compileClasspathConfigurationName,
        )
    }

    @Test
    fun testJavaSourceSetOutputShouldNotBeIncludedInFinalArtifact() {
        baseJvmProject.evaluate()

        val jvmMainCompilation = baseJvmProject.multiplatformExtension.jvmMainCompilation
        val jvmTestCompilation = baseJvmProject.multiplatformExtension.jvmTestCompilation
        assertFalse(
            jvmMainCompilation.output.classesDirs.files.containsAll(
                baseJvmProject.jvmSourceSet(jvmTestCompilation).output.classesDirs.files
            )
        )
    }

    @Test
    fun testJavaCompileTaskIsIncludedAsClassesTaskDependency() {
        baseJvmProject.evaluate()

        val jvmTestCompilation = baseJvmProject.multiplatformExtension.jvmTestCompilation
        val jvmTestClassesTask = baseJvmProject.tasks.getByName(jvmTestCompilation.compileAllTaskName)
        val javaProcessResourcesTask = baseJvmProject.tasks.getByName(jvmTestCompilation.defaultJavaSourceSet.processResourcesTaskName)

        jvmTestClassesTask.assertNoCircularTaskDependencies()
        jvmTestClassesTask.assertTaskDependenciesEquals(
            setOf(
                jvmTestCompilation.compileTaskProvider.get(),
                jvmTestCompilation.defaultCompileJavaProvider.get(),
                baseJvmProject.tasks.getByName(jvmTestCompilation.processResourcesTaskName),
                javaProcessResourcesTask
            )
        )
        assertFalse(javaProcessResourcesTask.enabled, "Java process resources task '${javaProcessResourcesTask.name} should be disabled")
    }

    @Test
    fun shouldCreateJavaSourceSetForCustomCompilation() {
        val jvmCustomCompilation = baseJvmProject.multiplatformExtension.jvm().compilations.create("custom")

        baseJvmProject.evaluate()

        val customSourceSet = baseJvmProject.javaSourceSets.findByName(
            jvmCustomCompilation.defaultSourceSet.name
        )
        assertNotNull(customSourceSet)
        val srcDirs = customSourceSet.java.srcDirs
        assertTrue(
            srcDirs.size == 1,
            "Custom Java source set contains more than one SourceDirectorySet: $srcDirs"
        )
        assertTrue(
            srcDirs.single().endsWith("src/jvmCustom/java"),
            "Custom java source set does not use 'src/jvmTest/java' as SourceDirectorySet"
        )

        assertEquals(
            jvmCustomCompilation.compileDependencyConfigurationName,
            customSourceSet.compileClasspathConfigurationName,
        )
    }

    @Test
    fun customCompilationJavaSourceSetOutputShouldBeIncludedInFinalArtifact() {
        val jvmCustomCompilation = baseJvmProject.multiplatformExtension.jvm().compilations.create("custom")

        baseJvmProject.evaluate()

        assertTrue(
            jvmCustomCompilation.output.classesDirs.files.containsAll(
                baseJvmProject.jvmSourceSet(jvmCustomCompilation).output.classesDirs.files
            )
        )
    }

    @Test
    fun customCompilationJavaCompileTaskIsIncludedAsClassesTaskDependency() {
        val jvmCustomCompilation = baseJvmProject.multiplatformExtension.jvm().compilations.create("custom")

        baseJvmProject.evaluate()

        val jvmCustomClassesTask = baseJvmProject.tasks.getByName(jvmCustomCompilation.compileAllTaskName)
        val javaProcessResourcesTask = baseJvmProject.tasks.getByName(jvmCustomCompilation.defaultJavaSourceSet.processResourcesTaskName)

        jvmCustomClassesTask.assertNoCircularTaskDependencies()
        jvmCustomClassesTask.assertTaskDependenciesEquals(
            setOf(
                jvmCustomCompilation.compileTaskProvider.get(),
                jvmCustomCompilation.defaultCompileJavaProvider.get(),
                baseJvmProject.tasks.getByName(jvmCustomCompilation.processResourcesTaskName),
                javaProcessResourcesTask
            )
        )
        assertFalse(javaProcessResourcesTask.enabled, "Java process resources task '${javaProcessResourcesTask.name} should be disabled")
    }

    @Test
    fun registeringStandaloneJavaSourceSetShouldNotLeadToError() {
        baseJvmProject.javaSourceSets.create("custom")

        baseJvmProject.evaluate()
    }

    @Test
    fun couldCoExistWithAndroidTarget() {
        val jvmAndAndroidProject = buildProjectWithMPP {
            plugins.apply("android-library")
            kotlin {
                jvm()
                androidTarget()
            }

            androidExtension.configureDefaults()
        }

        jvmAndAndroidProject.evaluate()

        val mainSourceSet = jvmAndAndroidProject.javaSourceSets.findByName(
            jvmAndAndroidProject.multiplatformExtension.jvmMainCompilation.defaultSourceSet.name
        )
        assertNotNull(mainSourceSet)
        val testSourceSet = jvmAndAndroidProject.javaSourceSets.findByName(
            jvmAndAndroidProject.multiplatformExtension.jvmTestCompilation.defaultSourceSet.name
        )
        assertNotNull(testSourceSet)
    }

    private val KotlinMultiplatformExtension.jvmMainCompilation
        get() = jvm().compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME) as KotlinJvmCompilation

    private val KotlinMultiplatformExtension.jvmTestCompilation
        get() = jvm().compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME) as KotlinJvmCompilation

    private fun Project.jvmSourceSet(compilation: KotlinJvmCompilation) =
        javaSourceSets.getByName(compilation.defaultSourceSet.name)
}