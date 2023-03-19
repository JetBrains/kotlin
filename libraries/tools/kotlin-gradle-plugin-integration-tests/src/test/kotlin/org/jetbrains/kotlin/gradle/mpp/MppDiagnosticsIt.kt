/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
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

    @GradleTest
    fun testReportTargetsOfTheSamplePlatformAndWithTheSameAttributes(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib-gradle-kotlin-dsl", gradleVersion) {
            // A hack to make project compatible with GradleTestKit infrastructure
            buildGradleKts.replaceText(
                """id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")""",
                """id("org.jetbrains.kotlin.multiplatform")""",
            )
            buildGradleKts.appendText("""
                
                val distinguishAttribute = Attribute.of(String::class.java) 
                fun org.jetbrains.kotlin.gradle.plugin.KotlinTarget.applyDistinguishingAttributeIfSet(value: String) {
                    if (project.properties.containsKey("applyDistinguishingAttribute")) {
                        attributes { 
                            attribute(distinguishAttribute, value)
                        }
                    }
                }
                kotlin {
                    jvm("jvm2") { applyDistinguishingAttributeIfSet("jvm2") }
                    linuxArm64("linuxArm_A") { applyDistinguishingAttributeIfSet("linuxArm_A") }
                    linuxArm64("linuxArm_B") { applyDistinguishingAttributeIfSet("linuxArm_B") }
                }
            """.trimIndent())

            val warningMessage = """w: The following targets are not distinguishable:
                    |  * 'jvm2', 'jvm6'
                    |  * 'linuxArm_A', 'linuxArm_B'""".trimMargin()

            build {
                assertOutputContains(warningMessage)
            }

            build(buildOptions = defaultBuildOptions.copy(freeArgs = listOf("-PapplyDistinguishingAttribute"))) {
                assertOutputDoesNotContain(warningMessage)
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
