/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.dependencies
import org.gradle.api.tasks.testing.Test as TestTask
import org.gradle.kotlin.dsl.repositories
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.javaSourceSets
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.targets
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestFixturesTest {
    @Test
    fun testNoDuplicatedClasspathInJvm() = testNoDuplicatedResourcesInClasspath(
        buildProjectWithJvm(
            projectBuilder = {
                withName("KT-68278")
            }
        )
    )

    @Test
    fun testNoDuplicatedClasspathInKmp() = testNoDuplicatedResourcesInClasspath(
        buildProjectWithMPP(
            projectBuilder = {
                withName("KT-68278")
            }) {
            kotlin {
                jvm {
                    @Suppress("DEPRECATION")
                    withJava()
                }
            }
        },
        "jvm"
    )

    @Test
    fun kt75262PluginApplicationOrderInJvm() {
        val project = buildProjectWithJvm(
            preApplyCode = { plugins.apply("java-test-fixtures") }
        )

        project.evaluate()
    }

    @Test
    fun kt75262PluginApplicationOrderInKMP() {
        val project = buildProjectWithMPP(
            preApplyCode = { plugins.apply("java-test-fixtures") }
        ) {
            kotlin {
                jvm()
            }
        }

        project.evaluate()
    }

    @Test
    fun kt75808testFixtureDependenciesAreExposedCorrectly() {
        val project = buildProjectWithMPP(
            preApplyCode = { plugins.apply("java-test-fixtures") }
        ) {
            kotlin {
                jvm()
            }

            dependencies {
                "testFixturesApi"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                "jvmTestFixturesApi"("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
            }
        }

        project.evaluate()

        val testFixturesApiElements = project.configurations.getByName("testFixturesApiElements")
        val allDeps = testFixturesApiElements.allDependencies
        assertEquals(2, testFixturesApiElements.allDependencies.count())
        assertTrue(
            actual = allDeps.any { dependency ->
                dependency.group == "org.jetbrains.kotlinx" && dependency.name == "kotlinx-coroutines-core"
            },
            message = "Outgoing configuration does not contain 'org.jetbrains.kotlinx:kotlinx-coroutines-core' dependency"
        )
        assertTrue(
            actual = allDeps.any { dependency ->
                dependency.group == "org.jetbrains.kotlinx" && dependency.name == "kotlinx-serialization-json"
            },
            message = "Outgoing configuration does not contain 'org.jetbrains.kotlinx:kotlinx-serialization-json' dependency"
        )
    }

    @Test
    fun kt75808testFixtureClassesAreExposedCorrectly() {
        val project = buildProjectWithMPP(
            preApplyCode = { plugins.apply("java-test-fixtures") }
        ) {
            kotlin {
                jvm()
            }

            dependencies {
                "testFixturesApi"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                "jvmTestFixturesApi"("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
            }
        }

        project.evaluate()

        val testFixturesCompilation = project.kotlinExtension
            .targets.single { it is KotlinJvmTarget }
            .compilations.getByName("testFixtures")
        val outputs = testFixturesCompilation.output.classesDirs.files
        val testFixturesSourceSet = project.javaSourceSets.getByName("testFixtures")
        assertTrue(
            actual = testFixturesSourceSet.output.classesDirs.files.containsAll(outputs),
            message = "Expected 'testFixtures' source set to contain ${outputs.joinToString { it.absolutePath }}, but actual content is  " +
                    testFixturesSourceSet.output.classesDirs.files.joinToString { it.absolutePath }
        )
    }

    private fun testNoDuplicatedResourcesInClasspath(project: ProjectInternal, targetPrefix: String? = null) = with(project) {
        plugins.apply("java-test-fixtures")
        repositories {
            mavenLocal()
            mavenCentralCacheRedirector()
        }
        evaluate()
        val testClasspath = (project.tasks.withType<TestTask>().findByName(lowerCamelCaseName(targetPrefix, "test"))
            ?: error("No test task")).classpath.files
        val jar = project.tasks.withType<Jar>().findByName(lowerCamelCaseName(targetPrefix, "jar")) ?: error("No jar task")
        val resourcesDirectory = project.tasks.withType<ProcessResources>().findByName(lowerCamelCaseName(targetPrefix, "processResources"))
            ?: error("No process resources task")
        val mainResourcesEntries = testClasspath.filter {
            it == jar.archiveFile.get().asFile || it == resourcesDirectory.destinationDir
        }
        assert(mainResourcesEntries.size == 1) {
            "Expected to see the main resources entry once, but got:\n${testClasspath.joinToString(separator = "\n")}"
        }
    }
}