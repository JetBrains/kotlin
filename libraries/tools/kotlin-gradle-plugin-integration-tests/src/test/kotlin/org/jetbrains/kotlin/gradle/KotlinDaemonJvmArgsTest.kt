/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.tooling.internal.consumer.ConnectorServices
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DaemonsGradlePluginTests
@DisplayName("Kotlin daemon JVM args")
class KotlinDaemonJvmArgsTest : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)

    @AfterEach
    internal fun tearDown() {
        // Stops Gradle and Kotlin daemon, so new run will pick up new jvm arguments
        ConnectorServices.reset()
    }

    @GradleTest
    @DisplayName("Kotlin daemon by default should inherit Gradle daemon max jvm heap size")
    internal fun shouldInheritGradleDaemonArgsByDefault(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            gradleProperties.append(
                """
                org.gradle.jvmargs = -Xmx758m
                """.trimIndent()
            )

            build("assemble") {
                assertKotlinDaemonJvmOptions(
                    listOf("-Xmx758m")
                )
            }
        }
    }

    @DisplayName("Kotlin daemon should allow to define own jvm options via gradle daemon jvm args system property")
    @GradleTest
    internal fun shouldAllowToRedefineViaDGradleOption(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            gradleProperties.append(
                """
                org.gradle.jvmargs =-Xmx758m -Dkotlin.daemon.jvm.options=Xmx1g,Xms128m
                """.trimIndent()
            )

            build("assemble") {
                assertKotlinDaemonJvmOptions(
                    listOf("-Xmx1g", "--Xms128m")
                )
            }
        }
    }

    @DisplayName("Jvm args defined in gradle.properties should override Gradle daemon jvm arguments inheritance")
    @GradleTest
    internal fun shouldUseArgumentsFromGradleProperties(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            gradleProperties.append(
                """
                org.gradle.jvmargs =-Xmx758m -Xms128m
                kotlin.daemon.jvmargs = -Xmx486m -Xms256m
                """.trimIndent()
            )

            build("assemble") {
                assertKotlinDaemonJvmOptions(
                    listOf("-Xmx486m", "--Xms256m")
                )
            }
        }
    }

    @DisplayName("Should use arguments from extension DSL")
    @GradleTest
    internal fun shouldUseDslArguments(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            gradleProperties.append(
                """
                org.gradle.jvmargs =-Xmx758m -Xms128m
                """.trimIndent()
            )

            //language=Groovy
            rootBuildGradle.append(
                """
                
                kotlin {
                    kotlinDaemonJvmArgs = ["-Xmx486m", "-Xms256m", "-Duser.country=US"]
                }
                
                """.trimIndent()
            )

            build("assemble") {
                assertKotlinDaemonJvmOptions(
                    listOf("-Xmx486m", "--Xms256m", "--Duser.country=US")
                )
            }
        }
    }

    @DisplayName("Should allow to override global arguments for specific task")
    @GradleTest
    internal fun allowOverrideArgsForSpecificTask(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion
        ) {
            gradleProperties.append(
                """
                org.gradle.jvmargs = -Xmx758m -Xms128m
                kotlin.daemon.jvmargs = -Xmx486m -Xms256m
                """.trimIndent()
            )

            //language=Groovy
            rootBuildGradle.append(
                """
                
                import org.jetbrains.kotlin.gradle.tasks.CompileUsingKotlinDaemon
                tasks
                    .matching {
                        it.name == "compileTestKotlin" && it instanceof CompileUsingKotlinDaemon 
                    }
                    .configureEach {
                        kotlinDaemonJvmArguments.set(["-Xmx1g", "-Xms512m"])                        
                    }
                
                """.trimIndent()
            )

            build("build") {
                assertKotlinDaemonJvmOptions(
                    listOf("-Xmx486m", "--Xms256m")
                )
                assertKotlinDaemonJvmOptions(
                    listOf("-Xmx1g", "--Xms512m")
                )
            }
        }
    }

    @DisplayName("Should inherit Gradle memory settings if it is not set in Kotlin daemon jvm args")
    @GradleTest
    fun inheritGradleMemorySettingsIfKotlinArgsNotContain(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            gradleProperties.append(
                """
                org.gradle.jvmargs =-Xmx758m -Xms128m
                """.trimIndent()
            )

            //language=Groovy
            rootBuildGradle.append(
                """
                
                kotlin {
                    kotlinDaemonJvmArgs = ["-Duser.country=US"]
                }
                
                """.trimIndent()
            )

            build("assemble") {
                assertKotlinDaemonJvmOptions(
                    listOf("-Xmx758m", "--Duser.country=US")
                )
            }
        }
    }

    private fun BuildResult.assertKotlinDaemonJvmOptions(
        expectedOptions: List<String>
    ) {
        val jvmArgsCommonMessage = "Kotlin compile daemon JVM options: "
        assertOutputContains(jvmArgsCommonMessage)
        val argsRegex = "\\[.+?]".toRegex()
        val argsStrings = output.lineSequence()
            .filter { it.contains(jvmArgsCommonMessage) }
            .map {
                argsRegex.findAll(it).last().value.removePrefix("[").removeSuffix("]").split(", ")
            }
        val containsArgs = argsStrings.any {
            it.containsAll(expectedOptions)
        }

        assert(containsArgs) {
            printBuildOutput()

            "${argsStrings.toList()} does not contain expected args: $expectedOptions"
        }
    }
}
