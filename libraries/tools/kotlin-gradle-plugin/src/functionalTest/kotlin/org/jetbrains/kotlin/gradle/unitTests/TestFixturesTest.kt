/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.api.tasks.testing.Test as TestTask
import org.gradle.kotlin.dsl.repositories
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.javaSourceSets
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.targets
import kotlin.test.Test
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
    fun kt77466testFixturesDependenciesAreCorrectlyPropagatedToCompilation() {
        val project = buildProjectWithMPP(
            preApplyCode = { plugins.apply("java-test-fixtures") }
        ) {
            kotlin {
                jvm()
            }

            dependencies {
                "testFixturesApi"("example:A:1.0.0")
                "jvmTestFixturesApi"("example:B:1.0.0")
                "testFixturesImplementation"("example:C:1.0.0")
                "jvmTestFixturesImplementation"("example:D:1.0.0")
                "testFixturesCompileOnly"("example:E:1.0.0")
                "jvmTestFixturesCompileOnly"("example:F:1.0.0")
            }
        }

        project.evaluate()

        val expectedDeps = setOf(
            Triple("example", "A", "1.0.0"),
            Triple("example", "B", "1.0.0"),
            Triple("example", "C", "1.0.0"),
            Triple("example", "D", "1.0.0"),
            Triple("example", "E", "1.0.0"),
            Triple("example", "F", "1.0.0"),
        )

        project.configurations
            .getByName(project.javaSourceSets["testFixtures"].compileClasspathConfigurationName)
            .assertDependenciesPresent(expectedDeps)

        project.configurations
            .getByName(
                project.multiplatformExtension.targets.getByName("jvm").compilations.getByName("testFixtures").compileDependencyConfigurationName
            )
            .assertDependenciesPresent(expectedDeps)
    }

    @Test
    fun kt77466testFixturesDependenciesAreCorrectlyPropagatedToRuntime() {
        val project = buildProjectWithMPP(
            preApplyCode = { plugins.apply("java-test-fixtures") }
        ) {
            kotlin {
                jvm()
            }

            dependencies {
                "testFixturesApi"("example:A:1.0.0")
                "jvmTestFixturesApi"("example:B:1.0.0")
                "testFixturesImplementation"("example:C:1.0.0")
                "jvmTestFixturesImplementation"("example:D:1.0.0")
                "testFixturesRuntimeOnly"("example:E:1.0.0")
                "jvmTestFixturesRuntimeOnly"("example:F:1.0.0")
            }
        }

        project.evaluate()

        val expectedDeps = setOf(
            Triple("example", "A", "1.0.0"),
            Triple("example", "B", "1.0.0"),
            Triple("example", "C", "1.0.0"),
            Triple("example", "D", "1.0.0"),
            Triple("example", "E", "1.0.0"),
            Triple("example", "F", "1.0.0"),
        )

        project.configurations
            .getByName(project.javaSourceSets["testFixtures"].runtimeClasspathConfigurationName)
            .assertDependenciesPresent(expectedDeps)

        project.configurations
            .getByName(
                project.multiplatformExtension.targets.getByName("jvm").compilations.getByName("testFixtures").runtimeDependencyConfigurationName!!
            )
            .assertDependenciesPresent(expectedDeps)
    }

    @Test
    fun kt75808testFixtureDependenciesAreCorrectlyPropagatedToApiElements() {
        val project = buildProjectWithMPP(
            preApplyCode = { plugins.apply("java-test-fixtures") }
        ) {
            kotlin {
                jvm()
            }

            dependencies {
                "testFixturesApi"("example:A:1.0.0")
                "jvmTestFixturesApi"("example:B:1.0.0")
                "testFixturesImplementation"("example:C:1.0.0")
                "jvmTestFixturesImplementation"("example:D:1.0.0")
                "testFixturesRuntimeOnly"("example:E:1.0.0")
                "jvmTestFixturesRuntimeOnly"("example:F:1.0.0")
            }
        }

        project.evaluate()

        val expectedDeps = setOf(
            Triple("example", "A", "1.0.0"),
            Triple("example", "B", "1.0.0"),
        )

        project.configurations
            .getByName(project.javaSourceSets["testFixtures"].apiElementsConfigurationName)
            .assertDependenciesPresent(expectedDeps, shouldIncludeProjectDependencies = false)
    }

    @Test
    fun kt75808testFixtureDependenciesAreCorrectlyPropagatedToRuntimeElements() {
        val project = buildProjectWithMPP(
            preApplyCode = { plugins.apply("java-test-fixtures") }
        ) {
            kotlin {
                jvm()
            }

            dependencies {
                "testFixturesApi"("example:A:1.0.0")
                "jvmTestFixturesApi"("example:B:1.0.0")
                "testFixturesImplementation"("example:C:1.0.0")
                "jvmTestFixturesImplementation"("example:D:1.0.0")
                "testFixturesRuntimeOnly"("example:E:1.0.0")
                "jvmTestFixturesRuntimeOnly"("example:F:1.0.0")
            }
        }

        project.evaluate()

        val expectedDeps = setOf(
            Triple("example", "A", "1.0.0"),
            Triple("example", "B", "1.0.0"),
            Triple("example", "C", "1.0.0"),
            Triple("example", "D", "1.0.0"),
            Triple("example", "E", "1.0.0"),
            Triple("example", "F", "1.0.0"),
        )

        project.configurations
            .getByName(project.javaSourceSets["testFixtures"].runtimeElementsConfigurationName)
            .assertDependenciesPresent(expectedDeps)
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


    private fun Configuration.assertDependenciesPresent(
        expectedDeps: Set<Triple<String, String, String>>,
        shouldIncludeProjectDependencies: Boolean = true
    ) {
        val kotlinTestFixturesAllDeps = allDependencies
        assertEquals(
            expectedDeps.size + if (shouldIncludeProjectDependencies) 2 else 0,
            kotlinTestFixturesAllDeps.size,
            message = "Actual content of configuration: ${kotlinTestFixturesAllDeps.joinToString { "${it.group}:${it.name}" }}"
        )
        expectedDeps.forEach { expectedDependency ->
            assertTrue(
                actual = kotlinTestFixturesAllDeps.any { dependency ->
                    dependency.group == expectedDependency.first &&
                            dependency.name == expectedDependency.second &&
                            dependency.version == expectedDependency.third
                },
                message = "Outgoing configuration does not contain ''$expectedDependency' dependency"
            )
        }
    }
}
