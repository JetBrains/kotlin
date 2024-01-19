/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import kotlinx.validation.api.buildGradleKts
import kotlinx.validation.api.resolve
import kotlinx.validation.api.test
import org.assertj.core.api.Assertions
import org.junit.Test
import kotlin.test.assertTrue

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
                arguments.add(":apiDump")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiDump")

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
                arguments.add(":apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
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
                arguments.add(":apiDump")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiDump")

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
                arguments.add(":apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
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
                arguments.add(":apiDump")
            }
        }

        runner.buildAndFail().apply {
            Assertions.assertThat(output).contains("apiDumpDirectory (\"../api\") should be inside the project directory")
        }
    }
}
