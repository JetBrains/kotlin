/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test.legacy

import org.jetbrains.abi.tools.test.api.*
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertTrue

class PublicMarkersTest : BaseKotlinGradleTest() {

    @Test
    fun testPublicMarkers() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/publicMarkers/markers.gradle.kts")
            }

            kotlin("ClassWithPublicMarkers.kt") {
                resolve("/examples/classes/ClassWithPublicMarkers.kt")
            }

            kotlin("ClassInPublicPackage.kt") {
                resolve("/examples/classes/ClassInPublicPackage.kt")
            }

            apiFile(projectName = rootProjectDir.name) {
                resolve("/examples/classes/ClassWithPublicMarkers.dump")
            }

            runner {
                arguments.add(":checkLegacyAbi")
            }
        }

        runner.build().apply {
            println(output)
            assertTaskSuccess(":checkLegacyAbi")
        }
    }

    // Public markers are not supported in KLIB ABI dumps
    @Ignore // filters should be aligned with jvm
    @Test
    fun testPublicMarkersForNativeTargets() {
        val runner = test {
            settingsGradleKts {
                resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
            }

            buildGradleKts {
                resolve("/examples/gradle/base/withNativePlugin.gradle.kts")
                resolve("/examples/gradle/configuration/publicMarkers/markers.gradle.kts")
            }

            kotlin("ClassWithPublicMarkers.kt", sourceSet = "commonMain") {
                resolve("/examples/classes/ClassWithPublicMarkers.kt")
            }

            kotlin("ClassInPublicPackage.kt", sourceSet = "commonMain") {
                resolve("/examples/classes/ClassInPublicPackage.kt")
            }

            abiFile(projectName = "testproject") {
                resolve("/examples/classes/ClassWithPublicMarkers.klib.dump")
            }

            runner {
                arguments.add(":checkLegacyAbi")
            }
        }

        runner.withDebug(true).build().apply {
            assertTaskSuccess(":checkLegacyAbi")
        }
    }

    @Test
    fun testFiltrationByPackageLevelAnnotations() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/publicMarkers/packages.gradle.kts")
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

            val expected = readFileList("/examples/classes/AnnotatedPackage.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }
}
