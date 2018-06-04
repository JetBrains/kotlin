/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinProjectIT : BaseGradleIT() {

    @Test
    fun testKotlinProject() {
        val project = Project("kotlinProject")
        val kotlinProject = project.getModels(KotlinProject::class.java).getModel(":")!!

        kotlinProject.assertBasics("kotlinProject", defaultBuildOptions().kotlinVersion, KotlinProject.ProjectType.PLATFORM_JVM, "WARN")
        assertTrue(kotlinProject.expectedByDependencies.isEmpty())

        assertEquals(2, kotlinProject.sourceSets.size)
        val mainSourceSet = kotlinProject.sourceSets.find { it.name == "main" }!!
        val testSourceSet = kotlinProject.sourceSets.find { it.name == "test" }!!

        mainSourceSet.assertBasics("main", SourceSet.SourceSetType.PRODUCTION, emptySet())
        testSourceSet.assertBasics("test", SourceSet.SourceSetType.TEST, listOf("main"))

        assertEquals(2, mainSourceSet.sourceDirectories.size)
        assertEquals(1, mainSourceSet.sourceDirectories.filter { it.absolutePath.contains("src/main/kotlin") }.size)
        assertEquals(1, mainSourceSet.sourceDirectories.filter { it.absolutePath.contains("src/main/java") }.size)
        assertEquals(1, mainSourceSet.resourcesDirectories.size)
        assertEquals(1, mainSourceSet.resourcesDirectories.filter { it.absolutePath.contains("src/main/resources") }.size)
        assertTrue(mainSourceSet.classesOutputDirectory.absolutePath.contains("build/classes/kotlin/main"))
        assertTrue(mainSourceSet.resourcesOutputDirectory.absolutePath.contains("build/resources/main"))
        assertTrue(mainSourceSet.compilerArguments.compileClasspath.any { it.absolutePath.contains("guava") })
        assertFalse(mainSourceSet.compilerArguments.compileClasspath.any { it.absolutePath.contains("testng") })

        assertEquals(2, testSourceSet.sourceDirectories.size)
        assertEquals(1, testSourceSet.sourceDirectories.filter { it.absolutePath.contains("src/test/kotlin") }.size)
        assertEquals(1, testSourceSet.sourceDirectories.filter { it.absolutePath.contains("src/test/java") }.size)
        assertEquals(1, testSourceSet.resourcesDirectories.size)
        assertEquals(1, testSourceSet.resourcesDirectories.filter { it.absolutePath.contains("src/test/resources") }.size)
        assertTrue(testSourceSet.classesOutputDirectory.absolutePath.contains("build/classes/kotlin/test"))
        assertTrue(testSourceSet.resourcesOutputDirectory.absolutePath.contains("build/resources/test"))
        assertTrue(testSourceSet.compilerArguments.compileClasspath.any { it.absolutePath.contains("guava") })
        assertTrue(testSourceSet.compilerArguments.compileClasspath.any { it.absolutePath.contains("testng") })
    }

    @Test
    fun testKotlinJavaProject() {
        val project = Project("kotlinJavaProject")
        val kotlinProject = project.getModels(KotlinProject::class.java).getModel(":")!!

        kotlinProject.assertBasics(
            "kotlinJavaProject",
            defaultBuildOptions().kotlinVersion,
            KotlinProject.ProjectType.PLATFORM_JVM,
            "WARN"
        )

        assertEquals(3, kotlinProject.sourceSets.size)
        val mainSourceSet = kotlinProject.sourceSets.find { it.name == "main" }!!
        val deploySourceSet = kotlinProject.sourceSets.find { it.name == "deploy" }!!
        val testSourceSet = kotlinProject.sourceSets.find { it.name == "test" }!!

        mainSourceSet.assertBasics("main", SourceSet.SourceSetType.PRODUCTION, emptySet())
        deploySourceSet.assertBasics("deploy", SourceSet.SourceSetType.PRODUCTION, emptySet())
        testSourceSet.assertBasics("test", SourceSet.SourceSetType.TEST, listOf("main"))
    }

    @Test
    fun testMultiplatformProject() {
        val project = Project("multiplatformProject")
        val models = project.getModels(KotlinProject::class.java)

        val libKotlinProject = models.getModel(":lib")!!
        val libJsKotlinProject = models.getModel(":libJs")!!
        val libJvmKotlinProject = models.getModel(":libJvm")!!

        libKotlinProject.assertBasics("lib", defaultBuildOptions().kotlinVersion, KotlinProject.ProjectType.PLATFORM_COMMON, "WARN")
        libJsKotlinProject.assertBasics("libJs", defaultBuildOptions().kotlinVersion, KotlinProject.ProjectType.PLATFORM_JS, "WARN")
        libJvmKotlinProject.assertBasics("libJvm", defaultBuildOptions().kotlinVersion, KotlinProject.ProjectType.PLATFORM_JVM, "WARN")

        assertEquals(1, libJsKotlinProject.expectedByDependencies.size)
        assertTrue(libJsKotlinProject.expectedByDependencies.contains(":lib"))

        assertEquals(1, libJvmKotlinProject.expectedByDependencies.size)
        assertTrue(libJvmKotlinProject.expectedByDependencies.contains(":lib"))


        assertEquals(2, libJsKotlinProject.sourceSets.size)
        val mainJsSourceSet = libJsKotlinProject.sourceSets.find { it.name == "main" }!!
        val testJsSourceSet = libJsKotlinProject.sourceSets.find { it.name == "test" }!!

        mainJsSourceSet.assertBasics("main", SourceSet.SourceSetType.PRODUCTION, emptySet())
        testJsSourceSet.assertBasics("test", SourceSet.SourceSetType.TEST, listOf("main"))

        assertEquals(1, mainJsSourceSet.sourceDirectories.size)
        assertEquals(1, mainJsSourceSet.sourceDirectories.filter { it.absolutePath.contains("src/main/kotlin") }.size)
        assertEquals(1, mainJsSourceSet.resourcesDirectories.size)
        assertEquals(1, mainJsSourceSet.resourcesDirectories.filter { it.absolutePath.contains("src/main/resources") }.size)
        assertTrue(mainJsSourceSet.classesOutputDirectory.absolutePath.contains("build/classes/kotlin/main"))
        assertTrue(mainJsSourceSet.resourcesOutputDirectory.absolutePath.contains("build/resources/main"))

        assertEquals(1, testJsSourceSet.sourceDirectories.size)
        assertEquals(1, testJsSourceSet.sourceDirectories.filter { it.absolutePath.contains("src/test/kotlin") }.size)
        assertEquals(1, testJsSourceSet.resourcesDirectories.size)
        assertEquals(1, testJsSourceSet.resourcesDirectories.filter { it.absolutePath.contains("src/test/resources") }.size)
        assertTrue(testJsSourceSet.classesOutputDirectory.absolutePath.contains("build/classes/kotlin/test"))
        assertTrue(testJsSourceSet.resourcesOutputDirectory.absolutePath.contains("build/resources/test"))
    }

    @Test
    fun testCoroutinesProjectDSL() {
        val project = Project("coroutinesProjectDSL")
        val kotlinProject = project.getModels(KotlinProject::class.java).getModel(":")!!

        kotlinProject.assertBasics(
            "coroutinesProjectDSL",
            defaultBuildOptions().kotlinVersion,
            KotlinProject.ProjectType.PLATFORM_JVM,
            "ENABLE"
        )
    }

    companion object {

        private fun KotlinProject.assertBasics(
            expectedName: String,
            expectedKotlinVersion: String,
            expectedProjectType: KotlinProject.ProjectType,
            expectedCoroutines: String
        ) {
            assertEquals(1L, modelVersion)
            assertEquals(expectedName, name)
            assertEquals(expectedKotlinVersion, kotlinVersion)
            assertEquals(expectedProjectType, projectType)
            assertEquals(expectedCoroutines, experimentalFeatures.coroutines)
        }

        private fun SourceSet.assertBasics(
            expectedName: String,
            expectedType: SourceSet.SourceSetType,
            expectedFriendSourceSets: Collection<String>
        ) {
            assertEquals(expectedName, name)
            assertEquals(expectedType, type)
            assertEquals(expectedFriendSourceSets.toList(), friendSourceSets.toList())
        }
    }
}