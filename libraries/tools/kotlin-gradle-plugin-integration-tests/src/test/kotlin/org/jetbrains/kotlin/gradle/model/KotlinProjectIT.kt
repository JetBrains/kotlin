/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@DisplayName("Kotlin plugin model")
@OtherGradlePluginTests
class KotlinProjectIT : KGPBaseTest() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        androidVersion = TestVersions.AGP.AGP_34,
    )

    @DisplayName("Valid model is available in Kotlin only project")
    @GradleTest
    fun testKotlinProject(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            getModels<KotlinProject> {
                with(getModel(":")!!) {
                    assertBasics(
                        "kotlinProject",
                        defaultBuildOptions.kotlinVersion,
                        KotlinProject.ProjectType.PLATFORM_JVM,
                        "DEFAULT"
                    )
                    assertTrue(expectedByDependencies.isEmpty())

                    assertEquals(2, sourceSets.size)
                    val mainSourceSet = sourceSets.single { it.name == "main" }
                    val testSourceSet = sourceSets.single { it.name == "test" }

                    mainSourceSet.assertBasics("main", SourceSet.SourceSetType.PRODUCTION, emptySet())
                    testSourceSet.assertBasics("test", SourceSet.SourceSetType.TEST, listOf("main"))

                    assertEquals(2, mainSourceSet.sourceDirectories.size)
                    assertTrue(
                        mainSourceSet.sourceDirectories.contains(
                            projectPath.resolve("src/main/kotlin").toFile()
                        )
                    )
                    assertTrue(
                        mainSourceSet.sourceDirectories.contains(
                            projectPath.resolve("src/main/java").toFile()
                        )
                    )
                    assertEquals(1, mainSourceSet.resourcesDirectories.size)
                    assertTrue(
                        mainSourceSet.resourcesDirectories.contains(
                            projectPath.resolve("src/main/resources").toFile()
                        )
                    )
                    assertEquals(
                        projectPath.resolve("build/classes/kotlin/main").toFile(),
                        mainSourceSet.classesOutputDirectory
                    )
                    assertEquals(
                        projectPath.resolve("build/resources/main").toFile(),
                        mainSourceSet.resourcesOutputDirectory
                    )
                    assertTrue(mainSourceSet.compilerArguments.compileClasspath.any { it.absolutePath.contains("guava") })
                    assertFalse(mainSourceSet.compilerArguments.compileClasspath.any { it.absolutePath.contains("testng") })

                    assertEquals(2, testSourceSet.sourceDirectories.size)
                    assertTrue(testSourceSet.sourceDirectories.contains(projectPath.resolve("src/test/kotlin").toFile()))
                    assertTrue(testSourceSet.sourceDirectories.contains(projectPath.resolve("src/test/java").toFile()))
                    assertEquals(1, testSourceSet.resourcesDirectories.size)
                    assertTrue(testSourceSet.resourcesDirectories.contains(projectPath.resolve("src/test/resources").toFile()))
                    assertEquals(projectPath.resolve("build/classes/kotlin/test").toFile(), testSourceSet.classesOutputDirectory)
                    assertEquals(projectPath.resolve("build/resources/test").toFile(), testSourceSet.resourcesOutputDirectory)
                    assertTrue(testSourceSet.compilerArguments.compileClasspath.any { it.absolutePath.contains("guava") })
                    assertTrue(testSourceSet.compilerArguments.compileClasspath.any { it.absolutePath.contains("testng") })
                }
            }
        }
    }

    @DisplayName("Valid model in mixed Kotlin-Java project")
    @GradleTest
    fun testKotlinJavaProject(gradleVersion: GradleVersion) {
        project("kotlinJavaProject", gradleVersion) {
            getModels<KotlinProject> {
                with(getModel(":")!!) {
                    assertBasics(
                        "kotlinJavaProject",
                        defaultBuildOptions.kotlinVersion,
                        KotlinProject.ProjectType.PLATFORM_JVM,
                        "DEFAULT"
                    )

                    assertEquals(3, sourceSets.size)
                    val mainSourceSet = sourceSets.single { it.name == "main" }
                    val deploySourceSet = sourceSets.single { it.name == "deploy" }
                    val testSourceSet = sourceSets.single { it.name == "test" }

                    mainSourceSet.assertBasics("main", SourceSet.SourceSetType.PRODUCTION, emptySet())
                    deploySourceSet.assertBasics("deploy", SourceSet.SourceSetType.PRODUCTION, emptySet())
                    testSourceSet.assertBasics("test", SourceSet.SourceSetType.TEST, listOf("main"))
                }
            }
        }
    }

    @DisplayName("Valid model in multiplatform project")
    @GradleTest
    fun testMultiplatformProject(gradleVersion: GradleVersion) {
        project("multiplatformProject", gradleVersion) {
            getModels<KotlinProject> {
                val libKotlinProject = getModel(":lib")!!
                val libJsKotlinProject = getModel(":libJs")!!
                val libJvmKotlinProject = getModel(":libJvm")!!

                libKotlinProject.assertBasics(
                    "lib",
                    defaultBuildOptions.kotlinVersion,
                    KotlinProject.ProjectType.PLATFORM_COMMON,
                    "DEFAULT"
                )
                libJsKotlinProject.assertBasics(
                    "libJs",
                    defaultBuildOptions.kotlinVersion,
                    KotlinProject.ProjectType.PLATFORM_JS,
                    "DEFAULT"
                )
                libJvmKotlinProject.assertBasics(
                    "libJvm",
                    defaultBuildOptions.kotlinVersion,
                    KotlinProject.ProjectType.PLATFORM_JVM,
                    "DEFAULT"
                )

                assertEquals(1, libJsKotlinProject.expectedByDependencies.size)
                assertTrue(libJsKotlinProject.expectedByDependencies.contains(":lib"))

                assertEquals(1, libJvmKotlinProject.expectedByDependencies.size)
                assertTrue(libJvmKotlinProject.expectedByDependencies.contains(":lib"))


                assertEquals(2, libJsKotlinProject.sourceSets.size)
                val mainJsSourceSet = libJsKotlinProject.sourceSets.single { it.name == "main" }
                val testJsSourceSet = libJsKotlinProject.sourceSets.single { it.name == "test" }

                mainJsSourceSet.assertBasics("main", SourceSet.SourceSetType.PRODUCTION, emptySet())
                testJsSourceSet.assertBasics("test", SourceSet.SourceSetType.TEST, listOf("main"))

                assertEquals(1, mainJsSourceSet.sourceDirectories.size)
                assertTrue(mainJsSourceSet.sourceDirectories.contains(projectPath.resolve("libJs/src/main/kotlin").toFile()))
                assertEquals(1, mainJsSourceSet.resourcesDirectories.size)
                assertTrue(mainJsSourceSet.resourcesDirectories.contains(projectPath.resolve("libJs/src/main/resources").toFile()))
                assertEquals(projectPath.resolve("libJs/build/classes/kotlin/main").toFile(), mainJsSourceSet.classesOutputDirectory)
                assertEquals(projectPath.resolve("libJs/build/resources/main").toFile(), mainJsSourceSet.resourcesOutputDirectory)

                assertEquals(1, testJsSourceSet.sourceDirectories.size)
                assertTrue(testJsSourceSet.sourceDirectories.contains(projectPath.resolve("libJs/src/test/kotlin").toFile()))
                assertEquals(1, testJsSourceSet.resourcesDirectories.size)
                assertTrue(testJsSourceSet.resourcesDirectories.contains(projectPath.resolve("libJs/src/test/resources").toFile()))
                assertEquals(projectPath.resolve("libJs/build/classes/kotlin/test").toFile(), testJsSourceSet.classesOutputDirectory)
                assertEquals(projectPath.resolve("libJs/build/resources/test").toFile(), testJsSourceSet.resourcesOutputDirectory)
            }
        }
    }

    @DisplayName("Kotlin-android project model is valid")
    @GradleTest
    fun testAndroidProject(gradleVersion: GradleVersion) {
        project(
            "AndroidExtensionsProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Summary)
        ) {
            getModels<KotlinProject> {
                with(getModel(":app")!!) {
                    assertBasics(
                        "app",
                        defaultBuildOptions.kotlinVersion,
                        KotlinProject.ProjectType.PLATFORM_JVM,
                        "DEFAULT"
                    )

                    assertTrue(expectedByDependencies.isEmpty())
                    assertEquals(5, sourceSets.size)

                    val sourceSets = sourceSets.sortedBy { it.name }

                    sourceSets[0].verifySourceSet(
                        this@project,
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
                        this@project,
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
                        this@project,
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
                        this@project,
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
                        this@project,
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
            }
        }
    }

    private fun SourceSet.verifySourceSet(
        testProject: TestProject,
        name: String,
        type: SourceSet.SourceSetType,
        friends: List<String>,
        sources: List<String>,
        resources: List<String>,
        classesOutputDir: String,
        resourcesOutputDir: String
    ) {
        assertEquals(name, name)
        assertEquals(type, this.type)

        assertEquals(friends.size, friendSourceSets.size)
        assertEquals(friends, friendSourceSets)

        assertEquals(sources.size, sourceDirectories.size)
        assertEquals(sources.map { testProject.projectPath.resolve(it).toFile() }, sourceDirectories)

        assertEquals(resources.size, resourcesDirectories.size)
        assertEquals(resources.map { testProject.projectPath.resolve(it).toFile() }, resourcesDirectories)

        assertEquals(testProject.projectPath.resolve(classesOutputDir).toFile(), classesOutputDirectory)
        assertEquals(testProject.projectPath.resolve(resourcesOutputDir).toFile(), resourcesOutputDirectory)

        assertNotEquals(0, compilerArguments.currentArguments.size)
        assertNotEquals(0, compilerArguments.defaultArguments.size)
    }


    @DisplayName("Project with coroutines dsl enabled has valid model")
    @GradleTest
    fun testCoroutinesProjectDSL(gradleVersion: GradleVersion) {
        project("coroutinesProjectDSL", gradleVersion) {
            getModels<KotlinProject> {
                getModel(":")!!.assertBasics(
                    "coroutinesProjectDSL",
                    defaultBuildOptions.kotlinVersion,
                    KotlinProject.ProjectType.PLATFORM_JVM,
                    "ENABLE"
                )
            }
        }
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
