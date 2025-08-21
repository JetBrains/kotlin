/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test.legacy

import org.jetbrains.abi.tools.test.api.*
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.text.contains

class OutputDirectoryTests : BaseKotlinGradleTest() {
    @Test
    fun dumpIntoCustomDirectory() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/outputDirectory/different.gradle.kts")
            }

            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }
            dir("api") {
                file("letMeBe.txt") {
                }
            }

            runner {
                arguments.add(":updateLegacyAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":updateLegacyAbi")

            val dumpFile = rootProjectDir.resolve("custom").resolve("${rootProjectDir.name}.api")
            assertTrue(dumpFile.exists(), "api dump file ${dumpFile.path} should exist")

            val expected = readFileList("/examples/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(dumpFile.readText()).isEqualToIgnoringNewLines(expected)

            val fileInsideDir = rootProjectDir.resolve("api").resolve("letMeBe.txt")
            assertTrue(fileInsideDir.exists(), "existing api directory should not be overridden")
        }
    }

    @Test
    fun validateDumpFromACustomDirectory() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/outputDirectory/different.gradle.kts")
            }

            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }
            dir("custom") {
                file("${rootProjectDir.name}.api") {
                    resolve("/examples/classes/AnotherBuildConfig.dump")
                }
            }

            runner {
                arguments.add(":checkLegacyAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":checkLegacyAbi")
        }
    }

    @Test
    fun dumpIntoSubdirectory() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/outputDirectory/subdirectory.gradle.kts")
            }

            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }

            runner {
                arguments.add(":updateLegacyAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":updateLegacyAbi")

            val dumpFile = rootProjectDir.resolve("validation")
                .resolve("api")
                .resolve("${rootProjectDir.name}.api")

            assertTrue(dumpFile.exists(), "api dump file ${dumpFile.path} should exist")

            val expected = readFileList("/examples/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(dumpFile.readText()).isEqualToIgnoringNewLines(expected)
        }
    }

    @Test
    fun validateDumpFromASubdirectory() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/outputDirectory/subdirectory.gradle.kts")
            }

            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }
            dir("validation") {
                dir("api") {
                    file("${rootProjectDir.name}.api") {
                        resolve("/examples/classes/AnotherBuildConfig.dump")
                    }
                }
            }

            runner {
                arguments.add(":checkLegacyAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":checkLegacyAbi")
        }
    }

    @Test
    fun dumpIntoParentDirectory() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/outputDirectory/outer.gradle.kts")
            }

            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }

            runner {
                arguments.add(":updateLegacyAbi")
            }
        }

        runner.buildAndFail().apply {
            Assertions.assertThat(output).contains("'referenceDir' must be a subdirectory of the build root directory")
        }
    }
}
