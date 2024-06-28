/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.util.assertConfigurationsHaveTaskDependencies
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.*

class FatFrameworksTest {
    @Test
    fun `two apple frameworks get bundled to a fat framework`() {
        val project = buildProjectWithMPP {
            kotlin {
                iosX64 { binaries.framework("foo", listOf(DEBUG)) }
                iosArm64 { binaries.framework("foo", listOf(DEBUG)) }
            }
        }
        project.evaluate()
        project.assertConfigurationExists("fooDebugFrameworkIosX64")
        project.assertConfigurationExists("fooDebugFrameworkIosArm64")
        project.assertConfigurationExists("fooDebugFrameworkIosFat")
    }

    @Test
    fun `single binary framework doesn't produce a fat framework`() {
        val project = buildProjectWithMPP {
            kotlin {
                iosX64 { binaries.framework("foo", listOf(DEBUG)) }
            }
        }
        project.evaluate()
        project.assertConfigurationExists("fooDebugFrameworkIosX64")
        project.assertConfigurationDoesntExist("fooDebugFrameworkIosFat")
    }

    @Test
    fun `fat framework grouping -- different families`() = testFatFrameworkGrouping(
        "fooDebugFrameworkIosFat",
        "fooDebugFrameworkOsxFat",
    ) {
        iosX64 { binaries.framework("foo", listOf(DEBUG)) }
        iosArm64 { binaries.framework("foo", listOf(DEBUG)) }
        macosX64 { binaries.framework("foo", listOf(DEBUG)) }
        macosArm64 { binaries.framework("foo", listOf(DEBUG)) }
    }

    @Test
    fun `fat framework grouping -- different families and different names within one family`() = testFatFrameworkGrouping(
        "fooDebugFrameworkOsxFat",
    ) {
        iosX64 { binaries.framework("foo", listOf(DEBUG)) }
        iosArm64 { binaries.framework("bar", listOf(DEBUG)) }
        macosX64 { binaries.framework("foo", listOf(DEBUG)) }
        macosArm64 { binaries.framework("foo", listOf(DEBUG)) }
    }

    @Test
    fun `fat framework grouping -- build types intersection`() = testFatFrameworkGrouping(
        "fooReleaseFrameworkIosFat",
    ) {
        iosX64 { binaries.framework("foo", listOf(RELEASE)) }
        iosArm64 { binaries.framework("foo", listOf(DEBUG, RELEASE)) }
    }

    @Test
    fun `fat framework grouping -- multiple build types`() = testFatFrameworkGrouping(
        "fooReleaseFrameworkIosFat",
        "fooDebugFrameworkIosFat",
    ) {
        iosX64 { binaries.framework("foo", listOf(DEBUG, RELEASE)) }
        iosArm64 { binaries.framework("foo", listOf(DEBUG, RELEASE)) }
    }

    @Test
    fun `fat framework contains framework name attribute`() {
        val project = buildProjectWithMPP {
            kotlin {
                iosX64 {
                    binaries.framework("foo", listOf(DEBUG)) { baseName = "f1" }
                    binaries.framework("bar", listOf(DEBUG)) { baseName = "f2" }
                }

                iosArm64 {
                    binaries.framework("foo", listOf(DEBUG)) { baseName = "f1" }
                    binaries.framework("bar", listOf(DEBUG)) { baseName = "f2" }
                }
            }
        }
        project.evaluate()
        val barFat = project.assertConfigurationExists("barDebugFrameworkIosFat")
        val fooFat = project.assertConfigurationExists("fooDebugFrameworkIosFat")
        assertEquals("fooDebugFramework", fooFat.attributes.getAttribute(KotlinNativeTarget.kotlinNativeFrameworkNameAttribute))
        assertEquals("barDebugFramework", barFat.attributes.getAttribute(KotlinNativeTarget.kotlinNativeFrameworkNameAttribute))
    }

    @Test
    fun `consumable configurations of frameworks have correct dependencies on producing tasks`() {
        val project = buildProjectWithMPP {
            kotlin {
                iosX64 { binaries.framework("foo", listOf(DEBUG)) }
                iosArm64 { binaries.framework("foo", listOf(DEBUG)) }
            }
        }
        project.evaluate()

        project.assertConfigurationsHaveTaskDependencies(
            "fooDebugFrameworkIosX64",
            ":linkFooDebugFrameworkIosX64"
        )

        project.assertConfigurationsHaveTaskDependencies(
            "fooDebugFrameworkIosArm64",
            ":linkFooDebugFrameworkIosArm64"
        )

        project.assertConfigurationsHaveTaskDependencies(
            "fooDebugFrameworkIosFat",
            ":linkFooDebugFrameworkIosFat"
        )
    }

    @Test
    fun `universal framework task - correctly depends on link task frameworks - when framework's baseName is redefined`() {
        val linkTasks = mutableSetOf<Task>()
        var eagerlyCreatedTask: FatFrameworkTask? = null
        val project = buildProjectWithMPP {
            kotlin {
                // 1. Eagerly configure universal framework task
                eagerlyCreatedTask = tasks.create("testUniversalFrameworkTask", FatFrameworkTask::class.java)

                listOf(
                    iosX64(),
                    iosSimulatorArm64(),
                ).forEach {
                    it.binaries.framework(listOf(NativeBuildType.DEBUG)) {
                        linkTasks.add(linkTaskProvider.get())
                        // 2. Depend on a framework
                        eagerlyCreatedTask?.from(this)
                        // 3. Redefine framework's baseName
                        baseName = "foo"
                    }
                }
            }
        }.evaluate()

        val task = assertNotNull(eagerlyCreatedTask)

        assertEquals(
            linkTasks,
            task.taskDependencies.getDependencies(null),
        )
        assertEquals(
            listOf(
                project.layout.buildDirectory.file("bin/iosX64/debugFramework/foo.framework").get().asFile,
                project.layout.buildDirectory.file("bin/iosSimulatorArm64/debugFramework/foo.framework").get().asFile,
            ),
            task.frameworks.map { it.file }
        )
    }

    private fun testFatFrameworkGrouping(
        vararg allExpectedFatFrameworks: String,
        configureTargets: KotlinMultiplatformExtension.() -> Unit,
    ) {
        val project = buildProjectWithMPP {
            kotlin {
                configureTargets()
            }
        }
        project.evaluate()
        val allFatFrameworks = project.configurations.names.filter { it.endsWith("Fat") }.toSet()
        assertEquals(allExpectedFatFrameworks.toSet(), allFatFrameworks)
    }

    private fun Project.assertConfigurationDoesntExist(name: String) {
        val configuration = project.configurations.findByName(name)
        if (configuration != null) fail("'$name' configuration was not expected")
    }

    private fun Project.assertConfigurationExists(name: String): Configuration {
        return project.configurations.findByName(name) ?: fail("'$name' configuration was expected to be created")
    }
}