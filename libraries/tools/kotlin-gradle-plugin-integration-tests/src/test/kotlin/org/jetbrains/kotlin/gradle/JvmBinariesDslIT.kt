/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals

@OptIn(ExperimentalKotlinGradlePluginApi::class)
@DisplayName("KMP JVM target binaries DSL")
@MppGradlePluginTests
class JvmBinariesDslIT : KGPBaseTest() {

    @DisplayName("Default binary is runnable")
    @GradleTest
    fun defaultBinaryIsRunnable(gradleVersion: GradleVersion) {
        project("mppRunJvm", gradleVersion) {
            subProject("multiplatform").buildScriptInjection {
                kotlinMultiplatform.jvm {
                    binaries {
                        executable {
                            mainClass.set("JvmMainKt")
                        }
                    }
                }
            }

            build("runJvm") {
                assertTasksExecuted(":multiplatform:runJvm")
            }
        }
    }

    @DisplayName("Default binary distribution is runnable")
    @GradleTest
    fun defaultBinaryDistributionIsRunnable(gradleVersion: GradleVersion) {
        project("mppRunJvm", gradleVersion) {
            subProject("multiplatform").buildScriptInjection {
                kotlinMultiplatform.jvmToolchain(17)
                kotlinMultiplatform.jvm {
                    binaries {
                        executable {
                            mainClass.set("JvmMainKt")
                        }
                    }
                }
            }

            build("installJvmDist") {
                assertTasksExecuted(
                    ":multiplatform:jvmJar",
                    ":multiplatform:startScriptsForJvm",
                    ":multiplatform:installJvmDist",
                )

                assertDirectoryInProjectExists("multiplatform/build/install/multiplatform-jvm/bin")
                assertFileInProjectExists("multiplatform/build/install/multiplatform-jvm/bin/multiplatform")
                assertFileInProjectExists("multiplatform/build/install/multiplatform-jvm/bin/multiplatform.bat")
                assertDirectoryInProjectExists("multiplatform/build/install/multiplatform-jvm/lib")
            }

            val runScript = if (OS.WINDOWS.isCurrentOs) "multiplatform.bat" else "multiplatform"
            assertScriptExecutionIsSuccessful(projectPath.resolve("multiplatform/build/install/multiplatform-jvm/bin/$runScript"))
        }
    }

    @DisplayName("Custom test binary distribution is runnable")
    @GradleTest
    fun customTestBinaryDistributionIsRunnable(gradleVersion: GradleVersion) {
        project("mppRunJvm", gradleVersion) {
            val runCodeFile = subProject("multiplatform").kotlinSourcesDir("jvmMain").resolve("JvmMain.kt")
            val testRunCodeFile = subProject("multiplatform").kotlinSourcesDir("jvmTest").resolve("JvmTestMain.kt")
            testRunCodeFile.createParentDirectories()
            runCodeFile.copyTo(testRunCodeFile, overwrite = true)

            subProject("multiplatform").buildScriptInjection {
                kotlinMultiplatform.jvmToolchain(17)
                kotlinMultiplatform.jvm {
                    binaries {
                        executable(
                            KotlinCompilation.TEST_COMPILATION_NAME,
                            "custom"
                        ) {
                            mainClass.set("JvmTestMainKt")
                        }
                    }
                }
            }

            build("installJvmCustomDist") {
                assertTasksExecuted(
                    ":multiplatform:jvmTestJar",
                    ":multiplatform:startScriptsForJvmTestCustom",
                    ":multiplatform:installJvmTestCustomDist",
                )

                assertDirectoryInProjectExists("multiplatform/build/install/multiplatform-jvmTestCustom/bin")
                assertFileInProjectExists("multiplatform/build/install/multiplatform-jvmTestCustom/bin/multiplatform")
                assertFileInProjectExists("multiplatform/build/install/multiplatform-jvmTestCustom/bin/multiplatform.bat")
                assertDirectoryInProjectExists("multiplatform/build/install/multiplatform-jvmTestCustom/lib")
            }

            val runScript = if (OS.WINDOWS.isCurrentOs) "multiplatform.bat" else "multiplatform"
            assertScriptExecutionIsSuccessful(projectPath.resolve("multiplatform/build/install/multiplatform-jvmTestCustom/bin/$runScript"))
        }
    }

    @DisplayName("Distribution is possible to customize")
    @GradleTest
    fun distributionCustomization(gradleVersion: GradleVersion) {
        project("mppRunJvm", gradleVersion) {
            subProject("multiplatform").buildScriptInjection {
                val additionalDistSources = project.layout.projectDirectory.dir("customDist")
                kotlinMultiplatform.jvm {
                    binaries {
                        executable {
                            mainClass.set("JvmMainKt")
                            applicationDistribution.from(additionalDistSources) {
                                it.include("*.txt")
                            }
                        }
                    }
                }
            }

            val customDistPath = subProject("multiplatform").projectPath.resolve("customDist")
            customDistPath.createDirectories()
            customDistPath.resolve("one.txt").writeText("one")
            customDistPath.resolve("two.ignore").writeText("two")

            build("installJvmDist") {
                assertTasksExecuted(
                    ":multiplatform:jvmJar",
                    ":multiplatform:startScriptsForJvm",
                    ":multiplatform:installJvmDist",
                )

                assertDirectoryInProjectExists("multiplatform/build/install/multiplatform-jvm/bin")
                assertFileInProjectExists("multiplatform/build/install/multiplatform-jvm/bin/multiplatform")
                assertFileInProjectExists("multiplatform/build/install/multiplatform-jvm/bin/multiplatform.bat")
                assertFileInProjectExists("multiplatform/build/install/multiplatform-jvm/one.txt")
                assertFileInProjectNotExists("multiplatform/build/install/multiplatform-jvm/two.ignore")
                assertDirectoryInProjectExists("multiplatform/build/install/multiplatform-jvm/lib")
            }
        }
    }

    @DisplayName("Default binary with JPMS is runnable")
    @GradleTest
    fun defaultBinaryWithJpmsIsRunnable(gradleVersion: GradleVersion) {
        project("mppRunJvm", gradleVersion) {
            val jvmModuleInfoFile = subProject("jvm").javaSourcesDir().resolve("module-info.java")
            jvmModuleInfoFile.parent.toFile().mkdirs()
            jvmModuleInfoFile.writeText(
                """
                |module com.util {
                |    exports com.util;
                |    requires kotlin.stdlib;
                |    requires kotlinx.coroutines.core;
                |}
                """.trimMargin()
            )
            subProject("jvm").kotlinSourcesDir().resolve("Jvm.kt").modify {
                """
                |package com.util
                |$it
                """.trimMargin()
            }
            subProject("jvm").javaSourcesDir().resolve("com/util/Empty.java")
                .apply { parent.toFile().mkdirs() }
                .writeText(
                    """
                    package com.util;
                    public class Empty {}
                    """.trimIndent()
                )

            val kmpModuleInfoFile = subProject("multiplatform").javaSourcesDir("jvmMain").resolve("module-info.java")
            kmpModuleInfoFile.parent.toFile().mkdirs()
            kmpModuleInfoFile.writeText(
                //language=java
                """
                |module org.example {
                |    exports org.example;
                |    requires kotlin.stdlib;
                |    requires com.util;
                |}
                """.trimMargin()
            )

            subProject("multiplatform")
                .kotlinSourcesDir("jvmMain")
                .resolve("JvmMain.kt")
                .modify {
                    """
                    package org.example
                    
                    import com.util.Jvm
                    $it
                    """.trimIndent()
                }
            subProject("multiplatform")
                .kotlinSourcesDir("commonMain")
                .resolve("CommonMain.kt")
                .modify {
                    """
                    package org.example
                    
                    import com.util.Jvm
                    """.trimIndent()
                }

            subProject("multiplatform").javaSourcesDir("jvmMain").resolve("org/example/Empty.java")
                .apply { parent.toFile().mkdirs() }
                .writeText(
                    //language=java
                    """
                    package org.example;
                    public class Empty {}
                    """.trimIndent()
                )


            subProject("multiplatform").buildScriptInjection {
                kotlinMultiplatform.jvm {
                    withJava()
                    binaries {
                        executable {
                            mainClass.set("org.example.JvmMainKt")
                            mainModule.set("org.example")
                        }
                    }
                }

                java.modularity.inferModulePath.set(true)
            }

            build("runJvm") {
                assertTasksExecuted(":multiplatform:runJvm")
            }
        }
    }

    private fun assertScriptExecutionIsSuccessful(
        pathToScript: Path
    ) {
        val processBuilder = ProcessBuilder(pathToScript.toAbsolutePath().toString())
        val outputFile = File.createTempFile("script-output", ".txt")
        outputFile.deleteOnExit()
        processBuilder.redirectErrorStream(true)
        processBuilder.redirectOutput(outputFile)
        // Prevent the distribution script picking up the wrong JDK
        processBuilder.environment()["JAVA_HOME"] = System.getProperty("jdk17Home")
        val process = processBuilder.start()
        assertEquals(0, process.waitFor(), "Distribution run failed:\n${outputFile.readText()}")
    }
}