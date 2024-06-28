/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

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

    companion object {

        private fun KotlinProject.assertBasics(
            expectedName: String,
            expectedKotlinVersion: String,
            expectedProjectType: KotlinProject.ProjectType,
        ) {
            assertEquals(1L, modelVersion)
            assertEquals(expectedName, name)
            assertEquals(expectedKotlinVersion, kotlinVersion)
            assertEquals(expectedProjectType, projectType)
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
