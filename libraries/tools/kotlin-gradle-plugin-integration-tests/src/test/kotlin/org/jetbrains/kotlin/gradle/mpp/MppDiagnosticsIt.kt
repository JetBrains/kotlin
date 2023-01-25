/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import kotlin.io.path.appendText
import kotlin.io.path.writeText
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@MppGradlePluginTests
class MppDiagnosticsIt : KGPBaseTest() {
    @GradleTest
    fun testDeprecatedProperties(gradleVersion: GradleVersion) {
        project("mppDeprecatedProperties", gradleVersion) {
            checkDeprecatedProperties(isDeprecationExpected = false)

            this.gradleProperties.appendText(
                defaultFlags.entries.joinToString(
                    prefix = System.lineSeparator(),
                    postfix = System.lineSeparator(),
                    separator = System.lineSeparator(),
                ) { (prop, value) -> "$prop=$value" }
            )
            checkDeprecatedProperties(isDeprecationExpected = true)

            // remove the MPP plugin from the top-level project and check the warnings are still reported in subproject
            this.buildGradleKts.writeText("")
            checkDeprecatedProperties(isDeprecationExpected = true)

            this.gradleProperties.appendText("kotlin.mpp.deprecatedProperties.nowarn=true${System.lineSeparator()}")
            checkDeprecatedProperties(isDeprecationExpected = false)
        }
    }

    @GradleTest
    fun testCommonMainMustNotDependOnOtherSourceSets(gradleVersion: GradleVersion) {
        project("commonMainDependsOnAnotherSourceSet", gradleVersion) {
            build("tasks") {
                assertOutputContains("w: 'commonMain' source set can't depend on other source sets.")
            }

            build("tasks", buildOptions = defaultBuildOptions.copy(freeArgs = listOf("-PcommonSourceSetDependsOnNothing"))) {
                assertOutputDoesNotContain("w: 'commonMain' source set can't depend on other source sets.")
            }
        }
    }

    private fun TestProject.checkDeprecatedProperties(isDeprecationExpected: Boolean) {
        build {
            val assert: (Boolean, String) -> Unit = if (isDeprecationExpected) ::assertTrue else ::assertFalse
            val warnings = output.lines().filter { it.startsWith("w:") }.toSet()

            defaultFlags.keys.forEach { flag ->
                assert(
                    warnings.any { warning -> Regex(".*$flag.*is obsolete.*").matches(warning) },
                    "A deprecation warning for the '$flag' should have been reported",
                )
            }
        }
    }

    private val defaultFlags: Map<String, String>
        get() = mapOf(
            "kotlin.mpp.enableGranularSourceSetsMetadata" to "true",
            "kotlin.mpp.enableCompatibilityMetadataVariant" to "false",
            "kotlin.internal.mpp.hierarchicalStructureByDefault" to "true",
            "kotlin.mpp.hierarchicalStructureSupport" to "true",
            "kotlin.native.enableDependencyPropagation" to "false",
        )
}
