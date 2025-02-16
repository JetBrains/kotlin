/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test.legacy

import org.jetbrains.abi.tools.test.api.*
import org.jetbrains.abi.tools.test.api.Assertions.assertThat
import org.junit.Test
import java.io.File

internal class MultiPlatformSingleJvmTargetTest : BaseKotlinGradleTest() {
    private fun BaseKotlinScope.createProjectHierarchyWithPluginOnRoot() {
        settingsGradleKts {
            resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
        }
        buildGradleKts {
            resolve("/examples/gradle/base/multiplatformWithSingleJvmTarget.gradle.kts")
        }
    }

    @Test
    fun testApiCheckPasses() {
        val runner = test {
                createProjectHierarchyWithPluginOnRoot()
                runner {
                    arguments.add(":checkLegacyAbi")
                }

                dir("$API_DIR/") {
                    file("testproject.api") {
                        resolve("/examples/classes/Subsub1Class.dump")
                        resolve("/examples/classes/Subsub2Class.dump")
                    }
                }

                dir("src/jvmMain/kotlin") {}
                kotlin("Subsub1Class.kt", "commonMain") {
                    resolve("/examples/classes/Subsub1Class.kt")
                }
                kotlin("Subsub2Class.kt", "jvmMain") {
                    resolve("/examples/classes/Subsub2Class.kt")
                }

            }

        runner.build().apply {
            assertTaskSuccess(":checkLegacyAbi")
        }
    }

    @Test
    fun testApiCheckFails() {
        val runner = test {
                createProjectHierarchyWithPluginOnRoot()
                runner {
                    arguments.add("--continue")
                    arguments.add(":checkLegacyAbi")
                }

                dir("$API_DIR/") {
                    file("testproject.api") {
                        resolve("/examples/classes/Subsub2Class.dump")
                        resolve("/examples/classes/Subsub1Class.dump")
                    }
                }

                dir("src/jvmMain/kotlin") {}
                kotlin("Subsub1Class.kt", "commonMain") {
                    resolve("/examples/classes/Subsub1Class.kt")
                }
                kotlin("Subsub2Class.kt", "jvmMain") {
                    resolve("/examples/classes/Subsub2Class.kt")
                }

            }

        runner.buildAndFail().apply {
            assertTaskFailure(":checkLegacyAbi")
            assertTaskNotRun(":apiCheck")
            assertThat(output).contains("ABI check failed for project testproject")
        }
    }

    @Test
    fun testApiDumpPasses() {
        val runner = test {
                createProjectHierarchyWithPluginOnRoot()

                runner {
                    arguments.add(":updateLegacyAbi")
                }

                dir("src/jvmMain/kotlin") {}
                kotlin("Subsub1Class.kt", "commonMain") {
                    resolve("/examples/classes/Subsub1Class.kt")
                }
                kotlin("Subsub2Class.kt", "jvmMain") {
                    resolve("/examples/classes/Subsub2Class.kt")
                }

            }

        runner.build().apply {
            assertTaskSuccess(":updateLegacyAbi")

            val commonExpectedApi = readFileList("/examples/classes/Subsub1Class.dump")

            val mainExpectedApi = commonExpectedApi + "\n" + readFileList("/examples/classes/Subsub2Class.dump")
            assertThat(jvmApiDump.readText()).isEqualToIgnoringNewLines(mainExpectedApi)
        }
    }

    @Test
    fun testApiDumpPassesForEmptyProject() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/multiplatformWithSingleJvmTarget.gradle.kts")
            }

            runner {
                arguments.add(":updateLegacyAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":updateLegacyAbi")
        }
    }

    @Test
    fun testApiCheckPassesForEmptyProject() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/multiplatformWithSingleJvmTarget.gradle.kts")
            }

            emptyApiFile(projectName = rootProjectDir.name)

            runner {
                arguments.add(":checkLegacyAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":checkLegacyAbi")
        }
    }

    @Test
    fun testApiCheckFailsForEmptyProjectWithoutDumpFile() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/multiplatformWithSingleJvmTarget.gradle.kts")
            }

            runner {
                arguments.add(":checkLegacyAbi")
            }
        }

        runner.buildAndFail().apply {
            assertTaskFailure(":checkLegacyAbi")
            assertThat(output).contains(
                "Expected file with ABI declarations 'api${File.separator}${rootProjectDir.name}.api' does not exist"
            )
        }
    }

    private val jvmApiDump: File get() = rootProjectDir.resolve("$API_DIR/testproject.api")

}
