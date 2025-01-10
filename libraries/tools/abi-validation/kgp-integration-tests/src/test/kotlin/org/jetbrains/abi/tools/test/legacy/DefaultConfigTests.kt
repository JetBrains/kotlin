/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test.legacy

import org.jetbrains.abi.tools.test.api.*
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.*

internal class DefaultConfigTests : BaseKotlinGradleTest() {

    @Test
    fun `apiCheck should fail, when there is no api directory, even if there are no Kotlin sources`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
            }
            runner {
                arguments.add(":checkLegacyAbi")
            }
        }

        val projectName = rootProjectDir.name
        runner.buildAndFail().apply {
            Assertions.assertThat(output).contains(
                "Expected file with ABI declarations 'api${File.separator}$projectName.api' does not exist."
            ).contains(
                "You can run ':updateLegacyAbi' task to create or overwrite reference ABI declarations"
            )
            assertTaskFailure(":checkLegacyAbi")
        }
    }

    @Test
    fun `check should fail, when there is no api directory, even if there are no Kotlin sources`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
            }

            runner {
                arguments.add(":checkLegacyAbi")
            }
        }

        runner.buildAndFail().apply {
            assertTaskFailure(":checkLegacyAbi")
            assertTaskNotRun(":check") // checkLegacyAbi mustn't trigger check task
        }
    }

    @Test
    fun `apiCheck should succeed, when api-File is empty, but no kotlin files are included in SourceSet`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
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
    fun `apiCheck should succeed when public classes match api file`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
            }
            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }
            apiFile(projectName = rootProjectDir.name) {
                resolve("/examples/classes/AnotherBuildConfig.dump")
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
    fun `apiCheck should fail when public classes match api file ignoring case`() {
        Assume.assumeTrue(underlyingFsIsCaseSensitive())

        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
            }
            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }
            apiFile(projectName = rootProjectDir.name.uppercase()) {
                resolve("/examples/classes/AnotherBuildConfig.dump")
            }

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.buildAndFail().apply {
            assertTaskFailure(":apiCheck")
        }
    }

    @Test
    fun `apiCheck should fail, when a public class is not in api-File`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
            }

            kotlin("BuildConfig.kt") {
                resolve("/examples/classes/BuildConfig.kt")
            }

            emptyApiFile(projectName = rootProjectDir.name)

            runner {
                arguments.add(":checkLegacyAbi")
            }
        }

        runner.buildAndFail().apply {
            val dumpOutput =
                    "  @@ -1,1 +1,7 @@\n" +
                            "  +public final class com/company/BuildConfig {\n" +
                            "  +\tpublic fun <init> ()V\n" +
                            "  +\tpublic final fun function ()I\n" +
                            "  +\tpublic final fun getProperty ()I\n" +
                            "  +}"

            assertTaskFailure(":checkLegacyAbi")
            Assertions.assertThat(output).contains(dumpOutput)
        }
    }

    @Test
    fun `apiDump should create empty api file when there are no Kotlin sources`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
            }

            runner {
                arguments.add(":updateLegacyAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":updateLegacyAbi")

            assertTrue(rootProjectApiDump.exists(), "api dump file ${rootProjectApiDump.path} should exist")

            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines("")
        }
    }

    @Test
    fun `apiDump should create api file with the name of the project, respecting settings file`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
            }
            settingsGradleKts {
                resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
            }

            runner {
                arguments.add(":updateLegacyAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":updateLegacyAbi")

            val apiDumpFile = rootProjectDir.resolve("$API_DIR/testproject.api")
            assertTrue(apiDumpFile.exists(), "api dump file ${apiDumpFile.path} should exist")

            assertFalse(rootProjectApiDump.exists(), "api dump file ${rootProjectApiDump.path} should NOT exist " +
                "(based on project dir instead of custom name from settings)")

            Assertions.assertThat(apiDumpFile.readText()).isEqualToIgnoringNewLines("")
        }
    }

    @Test
    fun `apiDump should dump public classes`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
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

            assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

            val expected = readFileList("/examples/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }

    @Test
    fun `apiCheck should be run when we run check`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
            }

            emptyApiFile(projectName = rootProjectDir.name)

            runner {
                arguments.add(":check")
            }
        }

        runner.build().apply {
            assertTaskUpToDate(":check")
            assertTaskNotRun(":checkLegacyAbi")
        }
    }


    private fun underlyingFsIsCaseSensitive(): Boolean {
        val f = Files.createTempFile("UPPER", "UPPER").toFile()
        f.deleteOnExit()
        try {
            val lower = File(f.absolutePath.lowercase())
            return !lower.exists()
        } finally {
            f.delete()
        }
    }
}
