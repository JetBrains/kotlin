/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KotlinProjectIT : BaseGradleIT() {
    override fun defaultBuildOptions(): BuildOptions {
        return super.defaultBuildOptions().copy(
            androidGradlePluginVersion = AGPVersion.v3_4_1,
            androidHome = KtTestUtil.findAndroidSdk()
        )
    }

    @Test
    fun testKotlinProject() {
        val project = Project("kotlinProject")
        val kotlinProject = project.getModels(KotlinProject::class.java).getModel(":")!!

        kotlinProject.assertBasics(
            "kotlinProject",
            defaultBuildOptions().kotlinVersion,
            KotlinProject.ProjectType.PLATFORM_JVM,
            "DEFAULT"
        )
        assertTrue(kotlinProject.expectedByDependencies.isEmpty())

        assertEquals(2, kotlinProject.sourceSets.size)
        val mainSourceSet = kotlinProject.sourceSets.find { it.name == "main" }!!
        val testSourceSet = kotlinProject.sourceSets.find { it.name == "test" }!!

        mainSourceSet.assertBasics("main", SourceSet.SourceSetType.PRODUCTION, emptySet())
        testSourceSet.assertBasics("test", SourceSet.SourceSetType.TEST, listOf("main"))

        assertEquals(2, mainSourceSet.sourceDirectories.size)
        assertTrue(mainSourceSet.sourceDirectories.contains(project.projectDir.resolve("src/main/kotlin")))
        assertTrue(mainSourceSet.sourceDirectories.contains(project.projectDir.resolve("src/main/java")))
        assertEquals(1, mainSourceSet.resourcesDirectories.size)
        assertTrue(mainSourceSet.resourcesDirectories.contains(project.projectDir.resolve("src/main/resources")))
        assertEquals(project.projectDir.resolve("build/classes/kotlin/main"), mainSourceSet.classesOutputDirectory)
        assertEquals(project.projectDir.resolve("build/resources/main"), mainSourceSet.resourcesOutputDirectory)
        assertTrue(mainSourceSet.compilerArguments.compileClasspath.any { it.absolutePath.contains("guava") })
        assertFalse(mainSourceSet.compilerArguments.compileClasspath.any { it.absolutePath.contains("testng") })

        assertEquals(2, testSourceSet.sourceDirectories.size)
        assertTrue(testSourceSet.sourceDirectories.contains(project.projectDir.resolve("src/test/kotlin")))
        assertTrue(testSourceSet.sourceDirectories.contains(project.projectDir.resolve("src/test/java")))
        assertEquals(1, testSourceSet.resourcesDirectories.size)
        assertTrue(testSourceSet.resourcesDirectories.contains(project.projectDir.resolve("src/test/resources")))
        assertEquals(project.projectDir.resolve("build/classes/kotlin/test"), testSourceSet.classesOutputDirectory)
        assertEquals(project.projectDir.resolve("build/resources/test"), testSourceSet.resourcesOutputDirectory)
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
            "DEFAULT"
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

        libKotlinProject.assertBasics("lib", defaultBuildOptions().kotlinVersion, KotlinProject.ProjectType.PLATFORM_COMMON, "DEFAULT")
        libJsKotlinProject.assertBasics("libJs", defaultBuildOptions().kotlinVersion, KotlinProject.ProjectType.PLATFORM_JS, "DEFAULT")
        libJvmKotlinProject.assertBasics("libJvm", defaultBuildOptions().kotlinVersion, KotlinProject.ProjectType.PLATFORM_JVM, "DEFAULT")

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
        assertTrue(mainJsSourceSet.sourceDirectories.contains(project.projectDir.resolve("libJs/src/main/kotlin")))
        assertEquals(1, mainJsSourceSet.resourcesDirectories.size)
        assertTrue(mainJsSourceSet.resourcesDirectories.contains(project.projectDir.resolve("libJs/src/main/resources")))
        assertEquals(project.projectDir.resolve("libJs/build/classes/kotlin/main"), mainJsSourceSet.classesOutputDirectory)
        assertEquals(project.projectDir.resolve("libJs/build/resources/main"), mainJsSourceSet.resourcesOutputDirectory)

        assertEquals(1, testJsSourceSet.sourceDirectories.size)
        assertTrue(testJsSourceSet.sourceDirectories.contains(project.projectDir.resolve("libJs/src/test/kotlin")))
        assertEquals(1, testJsSourceSet.resourcesDirectories.size)
        assertTrue(testJsSourceSet.resourcesDirectories.contains(project.projectDir.resolve("libJs/src/test/resources")))
        assertEquals(project.projectDir.resolve("libJs/build/classes/kotlin/test"), testJsSourceSet.classesOutputDirectory)
        assertEquals(project.projectDir.resolve("libJs/build/resources/test"), testJsSourceSet.resourcesOutputDirectory)
    }

    @Test
    fun testAndroidProject() {
        val project = Project("AndroidExtensionsProject")
        val kotlinProject = project.getModels(KotlinProject::class.java).getModel(":app")!!

        kotlinProject.assertBasics(
            "app",
            defaultBuildOptions().kotlinVersion,
            KotlinProject.ProjectType.PLATFORM_JVM,
            "DEFAULT"
        )

        assertTrue(kotlinProject.expectedByDependencies.isEmpty())
        assertEquals(5, kotlinProject.sourceSets.size)

        val sourceSets = kotlinProject.sourceSets.sortedBy { it.name }

        fun SourceSet.verifySourceSet(
            name: String, type: SourceSet.SourceSetType, friends: List<String>, sources: List<String>, resources: List<String>,
            classesOutputDir: String, resourcesOutputDir: String
        ) {
            assertEquals(name, name)
            assertEquals(type, this.type)

            assertEquals(friends.size, friendSourceSets.size)
            assertEquals(friends, friendSourceSets)

            assertEquals(sources.size, sourceDirectories.size)
            assertEquals(sources.map { project.projectDir.resolve(it) }, sourceDirectories)

            assertEquals(resources.size, resourcesDirectories.size)
            assertEquals(resources.map { project.projectDir.resolve(it) }, resourcesDirectories)

            assertEquals(project.projectDir.resolve(classesOutputDir), classesOutputDirectory)
            assertEquals(project.projectDir.resolve(resourcesOutputDir), resourcesOutputDirectory)

            assertNotEquals(0, compilerArguments.currentArguments.size)
            assertNotEquals(0, compilerArguments.defaultArguments.size)
        }

        sourceSets[0].verifySourceSet(
            "debug",
            SourceSet.SourceSetType.PRODUCTION,
            listOf(),
            listOf(
                "app/src/debug/kotlin",
                "app/src/debug/java",
                "app/src/main/kotlin",
                "app/src/main/java"
            ),
            listOf(
                "app/src/debug/resources",
                "app/src/main/resources"
            ),
            "app/build/tmp/kotlin-classes/debug", "app/build/processedResources/debug"
        )
        sourceSets[1].verifySourceSet(
            "debugAndroidTest",
            SourceSet.SourceSetType.TEST,
            listOf("debug"),
            listOf(
                "app/src/debugAndroidTest/kotlin",
                "app/src/androidTest/kotlin",
                "app/src/androidTest/java",
                "app/src/androidTestDebug/kotlin",
                "app/src/androidTestDebug/java"
            ),
            listOf(
                "app/src/debugAndroidTest/resources",
                "app/src/androidTest/resources",
                "app/src/androidTestDebug/resources"
            ),
            "app/build/tmp/kotlin-classes/debugAndroidTest", "app/build/processedResources/debugAndroidTest"
        )
        sourceSets[2].verifySourceSet(
            "debugUnitTest",
            SourceSet.SourceSetType.TEST,
            listOf("debug"),
            listOf(
                "app/src/debugUnitTest/kotlin",
                "app/src/test/kotlin",
                "app/src/test/java",
                "app/src/testDebug/kotlin",
                "app/src/testDebug/java"
            ),
            listOf(
                "app/src/debugUnitTest/resources",
                "app/src/test/resources",
                "app/src/testDebug/resources"
            ),
            "app/build/tmp/kotlin-classes/debugUnitTest", "app/build/processedResources/debugUnitTest"
        )
        sourceSets[3].verifySourceSet(
            "release",
            SourceSet.SourceSetType.PRODUCTION,
            listOf(),
            listOf(
                "app/src/release/kotlin",
                "app/src/release/java",
                "app/src/main/kotlin",
                "app/src/main/java"
            ),
            listOf(
                "app/src/release/resources",
                "app/src/main/resources"
            ),
            "app/build/tmp/kotlin-classes/release", "app/build/processedResources/release"
        )
        sourceSets[4].verifySourceSet(
            "releaseUnitTest",
            SourceSet.SourceSetType.TEST,
            listOf("release"),
            listOf(
                "app/src/releaseUnitTest/kotlin",
                "app/src/test/kotlin",
                "app/src/test/java",
                "app/src/testRelease/kotlin",
                "app/src/testRelease/java"
            ),
            listOf(
                "app/src/releaseUnitTest/resources",
                "app/src/test/resources",
                "app/src/testRelease/resources"
            ),
            "app/build/tmp/kotlin-classes/releaseUnitTest", "app/build/processedResources/releaseUnitTest"
        )
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
