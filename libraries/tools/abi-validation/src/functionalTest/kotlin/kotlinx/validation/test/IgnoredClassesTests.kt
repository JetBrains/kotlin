/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import kotlinx.validation.api.BaseKotlinGradleTest
import kotlinx.validation.api.assertTaskSuccess
import kotlinx.validation.api.buildGradleKts
import kotlinx.validation.api.kotlin
import kotlinx.validation.api.readFileList
import kotlinx.validation.api.resolve
import kotlinx.validation.api.runner
import kotlinx.validation.api.test
import org.assertj.core.api.Assertions
import org.junit.Test
import kotlin.test.assertTrue

internal class IgnoredClassesTests : BaseKotlinGradleTest() {

    @Test
    fun `apiCheck should succeed, when given class is not in api-File, but is ignored via ignoredClasses`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin.gradle.kts")
                resolve("examples/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            }

            kotlin("BuildConfig.kt") {
                resolve("examples/classes/BuildConfig.kt")
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
    fun `apiCheck should succeed, when given class is not in api-File, but is ignored via ignoredPackages`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin.gradle.kts")
                resolve("examples/gradle/configuration/ignoredPackages/oneValidPackage.gradle.kts")
            }

            kotlin("BuildConfig.kt") {
                resolve("examples/classes/BuildConfig.kt")
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
    fun `apiDump should not dump ignoredClasses, when class is excluded via ignoredClasses`() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin.gradle.kts")
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

            assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

            val expected = readFileList("examples/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }
}
