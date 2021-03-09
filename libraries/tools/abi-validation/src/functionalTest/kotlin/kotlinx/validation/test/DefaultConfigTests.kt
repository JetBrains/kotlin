/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import org.assertj.core.api.*
import org.junit.Test
import kotlin.test.*

internal class DefaultConfigTests : BaseKotlinGradleTest() {

    @Test
    fun `apiCheck should fail, when there is no api directory, even if there are no Kotlin sources`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin.gradle.kts")
            }
            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.buildAndFail().apply {
            assertTrue { output.contains("Please ensure that ':apiDump' was executed") }
            assertTaskFailure(":apiCheck")
        }
    }

    @Test
    fun `check should fail, when there is no api directory, even if there are no Kotlin sources`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin.gradle.kts")
            }

            runner {
                arguments.add(":check")
            }
        }

        runner.buildAndFail().apply {
            assertTaskFailure(":apiCheck")
            assertTaskNotRun(":check") // apiCheck fails before we can run check
        }
    }

    @Test
    fun `apiCheck should succeed, when api-File is empty, but no kotlin files are included in SourceSet`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin.gradle.kts")
            }

            emptyApiFile(projectName = rootProjectDir.name)

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }

    @Test
    fun `apiCheck should succeed when public classes match api file`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin.gradle.kts")
            }
            kotlin("AnotherBuildConfig.kt") {
                resolve("examples/classes/AnotherBuildConfig.kt")
            }
            apiFile(projectName = rootProjectDir.name) {
                resolve("examples/classes/AnotherBuildConfig.dump")
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
    fun `apiCheck should fail, when a public class is not in api-File`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin.gradle.kts")
            }

            kotlin("BuildConfig.kt") {
                resolve("examples/classes/BuildConfig.kt")
            }

            emptyApiFile(projectName = rootProjectDir.name)

            runner {
                arguments.add(":apiCheck")
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

            assertTaskFailure(":apiCheck")
            Assertions.assertThat(output).contains(dumpOutput)
        }
    }

    @Test
    fun `apiDump should create empty api file when there are no Kotlin sources`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin.gradle.kts")
            }

            runner {
                arguments.add(":apiDump")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiDump")

            assertTrue(rootProjectApiDump.exists(), "api dump file ${rootProjectApiDump.path} should exist")

            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines("")
        }
    }

    @Test
    fun `apiDump should create api file with the name of the project, respecting settings file`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin.gradle.kts")
            }
            settingsGradleKts {
                resolve("examples/gradle/settings/settings-name-testproject.gradle.kts")
            }

            runner {
                arguments.add(":apiDump")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiDump")

            val apiDumpFile = rootProjectDir.resolve("api/testproject.api")
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
                resolve("examples/gradle/base/withPlugin.gradle.kts")
            }
            kotlin("AnotherBuildConfig.kt") {
                resolve("examples/classes/AnotherBuildConfig.kt")
            }

            runner {
                arguments.add(":apiDump")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiDump")

            assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

            val expected = readFileList("examples/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }

    @Test
    fun `apiCheck should be run when we run check`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin.gradle.kts")
            }

            emptyApiFile(projectName = rootProjectDir.name)

            runner {
                arguments.add(":check")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":check")
            assertTaskSuccess(":apiCheck")
        }
    }
}
