/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test

import org.jetbrains.abi.tools.test.api.*
import org.junit.Test
import kotlin.test.assertTrue

internal class IgnoredClassesTests : BaseKotlinGradleTest() {

    @Test
    fun `apiCheck should succeed, when given class is not in api-File, but is ignored via ignoredClasses`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            }

            kotlin("BuildConfig.kt") {
                resolve("/examples/classes/BuildConfig.kt")
            }

            emptyApiFile(projectName = rootProjectDir.name)

            runner {
                arguments.add(":checkKotlinAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":checkKotlinAbi")
        }
    }

    @Test
    fun `apiCheck should succeed, when given class is not in api-File, but is ignored via ignoredPackages`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/ignoredPackages/oneValidPackage.gradle.kts")
            }

            kotlin("BuildConfig.kt") {
                resolve("/examples/classes/BuildConfig.kt")
            }

            emptyApiFile(projectName = rootProjectDir.name)

            runner {
                arguments.add(":checkKotlinAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":checkKotlinAbi")
        }
    }

    @Test
    fun `apiDump should not dump ignoredClasses, when class is excluded via ignoredClasses`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            }
            kotlin("BuildConfig.kt") {
                resolve("/examples/classes/BuildConfig.kt")
            }
            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }

            runner {
                arguments.add(":updateKotlinAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":updateKotlinAbi")

            assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

            val expected = readFileList("/examples/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }

    @Test
    fun `apiDump should dump class whose name is a subsset of another class that is excluded via ignoredClasses`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            }
            kotlin("BuildConfig.kt") {
                resolve("/examples/classes/BuildConfig.kt")
            }
            kotlin("BuildCon.kt") {
                resolve("/examples/classes/BuildCon.kt")
            }

            runner {
                arguments.add(":updateKotlinAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":updateKotlinAbi")

            assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

            val expected = readFileList("/examples/classes/BuildCon.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }
}
