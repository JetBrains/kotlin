/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.BaseKotlinGradleTest
import kotlinx.validation.api.assertTaskFailure
import kotlinx.validation.api.assertTaskSuccess
import kotlinx.validation.api.buildGradleKts
import kotlinx.validation.api.kotlin
import kotlinx.validation.api.runner
import kotlinx.validation.api.readFileList
import kotlinx.validation.api.resolve
import kotlinx.validation.api.test
import org.assertj.core.api.Assertions
import org.junit.Test

internal class IgnoredClassesTest : BaseKotlinGradleTest() {
    @Test
    fun `apiCheck should succeed, when no kotlin files are included in SourceSet`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/default/build.gradle.kts")
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
    fun `apiCheck should succeed, when given class is not in api-File, but is ignored via ignoredClasses`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/default/build.gradle.kts")
                resolve("examples/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            }

            kotlin("BuildConfig.kt") {
                resolve("examples/classes/BuildConfig.kt")
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
                resolve("examples/gradle/default/build.gradle.kts")
            }

            kotlin("BuildConfig.kt") {
                resolve("examples/classes/BuildConfig.kt")
            }

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
    fun `apiCheck should succeed, when given class is not in api-File, but is ignored via ignoredPackages`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/default/build.gradle.kts")
                resolve("examples/gradle/configuration/ignoredPackages/oneValidPackage.gradle.kts")
            }

            kotlin("BuildConfig.kt") {
                resolve("examples/classes/BuildConfig.kt")
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
    fun `apiDump should not dump ignoredClasses, when class is excluded via ignoredClasses`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/default/build.gradle.kts")
                resolve("examples/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            }
            kotlin("BuildConfig.kt") {
                resolve("examples/classes/BuildConfig.kt")
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

            val expected = readFileList("examples/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(apiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }
}
