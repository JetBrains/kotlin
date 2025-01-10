/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test.legacy

import org.jetbrains.abi.tools.test.api.*
import org.junit.*
import kotlin.test.assertTrue

class NonPublicMarkersTest : BaseKotlinGradleTest() {

    @Test
    fun testIgnoredMarkersOnProperties() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/nonPublicMarkers/markers.gradle.kts")
            }

            kotlin("Properties.kt") {
                resolve("/examples/classes/Properties.kt")
            }

            apiFile(projectName = rootProjectDir.name) {
                resolve("/examples/classes/Properties.dump")
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
    fun testIgnoredMarkersOnPropertiesForNativeTargets() {
        val runner = test {
            settingsGradleKts {
                resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
            }

            buildGradleKts {
                resolve("/examples/gradle/base/withNativePlugin.gradle.kts")
                resolve("/examples/gradle/configuration/nonPublicMarkers/markers.gradle.kts")
            }

            kotlin("Properties.kt", sourceSet = "commonMain") {
                resolve("/examples/classes/Properties.kt")
            }
            abiFile(projectName = "testproject") {
                resolve("/examples/classes/Properties.klib.dump")
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
    fun testFiltrationByPackageLevelAnnotations() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/nonPublicMarkers/packages.gradle.kts")
            }
            java("annotated/PackageAnnotation.java") {
                resolve("/examples/classes/PackageAnnotation.java")
            }
            java("annotated/package-info.java") {
                resolve("/examples/classes/package-info.java")
            }
            kotlin("ClassFromAnnotatedPackage.kt") {
                resolve("/examples/classes/ClassFromAnnotatedPackage.kt")
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
    fun testIgnoredMarkersOnConstProperties() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/nonPublicMarkers/markers.gradle.kts")
            }

            kotlin("ConstProperty.kt") {
                resolve("/examples/classes/ConstProperty.kt")
            }

            apiFile(projectName = rootProjectDir.name) {
                resolve("/examples/classes/ConstProperty.dump")
            }

            runner {
                arguments.add(":checkLegacyAbi")
            }
        }

        runner.withDebug(true).build().apply {
            assertTaskSuccess(":checkLegacyAbi")
        }
    }
}
