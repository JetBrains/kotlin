/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("INVISIBLE_REFERENCE") // for LazyResolvedConfigurationComponent
package org.jetbrains.kotlin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.ConfigurationCacheValue
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.assertConfigurationCacheReused
import org.jetbrains.kotlin.gradle.testbase.assertConfigurationCacheStored
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationComponent
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertAll
import javax.inject.Inject

@DisplayName("LazyResolvedConfiguration utility tests")
@MppGradlePluginTests
class LazyResolvedConfigurationIT : KGPBaseTest() {

    internal abstract class MyTask @Inject constructor(
        private val lazyResolvedConfiguration: LazyResolvedConfigurationComponent
    ): DefaultTask() {
        @TaskAction
        fun action() {
            store[name] = lazyResolvedConfiguration.hashCode()
        }

        companion object {
            @JvmStatic
            val store = mutableMapOf<String, Int>()
        }
    }

    // FIXME: Actually objects SHOULD be the same even after deserialisation
    //  https://github.com/gradle/gradle/issues/38577
    @GradleTest
    fun `lazy resolved configuration is NOT the same after configuration cache deserialization`(
        gradleVersion: GradleVersion
    ) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(configurationCache = ConfigurationCacheValue.ENABLED)
        ) {
            plugins {
                kotlin("multiplatform") apply false
            }

            buildScriptInjection {
                // Gradle 7.6 doesn't have resolvable configurations yet
                val configuration = project.configurations.create("myResolvableConf")
                configuration.isCanBeConsumed = false

                val lazyResolvedConfiguration = LazyResolvedConfigurationComponent(configuration)

                project.dependencies.add(
                    "myResolvableConf",
                    "org.jetbrains.kotlin:kotlin-stdlib:2.4.0"
                )
                val taskA = project.tasks.register("taskA", MyTask::class.java, lazyResolvedConfiguration)
                val taskB = project.tasks.register("taskB", MyTask::class.java, lazyResolvedConfiguration)
                project.tasks.register("checkHashes") {
                    it.doFirst {
                        val taskAHash = MyTask.store["taskA"]
                        val taskBHash = MyTask.store["taskB"]
                        // FIXME: KT-87854 Add junit + kotlin.test to KGP IT buildScriptInjection's classpath
                        if (taskAHash != taskBHash) {
                            error("LazyResolvedConfigurationComponent is different in tasks: $taskAHash != $taskBHash")
                        }
                    }

                    it.dependsOn(taskA, taskB)
                }
            }

            assertAll(
                // First run
                {
                    // Since gradle 7.8 even on first run they "replicate" configuration cache restoration logic
                    // i.e. all transitives will be null, and objects deserialized
                    if (gradleVersion >= GradleVersion.version("7.8")) {
                        buildAndFail("checkHashes") {
                            assertConfigurationCacheStored()
                            assertOutputContains("LazyResolvedConfigurationComponent is different in tasks:")
                        }
                    } else {
                        build("checkHashes") {
                            assertConfigurationCacheStored()
                        }
                    }
                },
                // Second run
                {
                    buildAndFail("checkHashes") {
                        assertConfigurationCacheReused()
                        assertOutputContains("LazyResolvedConfigurationComponent is different in tasks:")
                    }
                }
            )
        }
    }
}
