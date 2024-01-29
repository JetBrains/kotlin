/*
 * Copyright 2016-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import org.assertj.core.api.Assertions
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
                arguments.add(":apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
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
                arguments.add(":apiDump")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiDump")

            assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

            val expected = readFileList("/examples/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }
}
