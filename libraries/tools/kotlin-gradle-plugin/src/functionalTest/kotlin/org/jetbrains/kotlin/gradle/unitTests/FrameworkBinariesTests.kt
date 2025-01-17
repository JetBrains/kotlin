/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.setAttribute
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast
import kotlin.test.Test
import kotlin.test.assertEquals

class FrameworkBinariesTests {

    @Test
    fun `assemble task dependencies includes framework tasks`() {
        val linkageTasksInAssemble = buildProjectWithMPP {
            kotlin {
                listOf(
                    // Thin linkage task
                    iosArm64(),
                    // These will also implicitly create a universal framework task
                    iosSimulatorArm64(),
                    iosX64(),
                ).forEach { it.binaries.framework() }
            }
        }.evaluate().tasks.getByName("assemble")
            .taskDependencies.getDependencies(null)
            .filter { it is KotlinNativeLink || it is FatFrameworkTask }
            .map { it.name }

        assertEquals(
            setOf(
                "linkDebugFrameworkIosArm64",
                "linkDebugFrameworkIosFat",
                "linkDebugFrameworkIosSimulatorArm64",
                "linkDebugFrameworkIosX64",
                "linkReleaseFrameworkIosArm64",
                "linkReleaseFrameworkIosFat",
                "linkReleaseFrameworkIosSimulatorArm64",
                "linkReleaseFrameworkIosX64",
            ),
            linkageTasksInAssemble.toSet()
        )
    }

    @Test
    fun `framework output file - reflects link task output file`() {
        buildProjectWithMPP {
            kotlin {
                iosSimulatorArm64 {
                    binaries.framework {
                        assertEquals(
                            "test.framework",
                            linkTaskProvider.get().outputFile.get().name,
                        )
                        assertEquals(
                            "test.framework",
                            outputFile.name,
                        )

                        baseName = "foo"

                        assertEquals(
                            "foo.framework",
                            linkTaskProvider.get().outputFile.get().name,
                        )
                        assertEquals(
                            "foo.framework",
                            outputFile.name,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `thin framework configurations - apply attributes at target and binary levels separately`() {
        val frameworkProducer = buildProjectWithMPP {
            kotlin {
                iosArm64 {
                    attributes.setAttribute(disambiguation1Attribute, "someValue")
                    binaries {
                        framework("main")
                        framework("custom") {
                            attributes.setAttribute(disambiguation2Attribute, "someValue2")
                        }
                    }
                }

                iosX64 {
                    binaries.framework("main")
                }
            }
        }.evaluate()

        data class ThinFrameworkTestCase(
            val target: String,
            val targetAttribute: String,
            val expectedDisambiguation1Attribute: String?,
            val expectedDisambiguation2Attribute: String?,
        )

        val testCases = listOf(
            ThinFrameworkTestCase(
                "iosArm64", "ios_arm64",
                expectedDisambiguation1Attribute = "someValue",
                expectedDisambiguation2Attribute = null,
            ),
            ThinFrameworkTestCase(
                "iosX64", "ios_x64",
                expectedDisambiguation1Attribute = null,
                expectedDisambiguation2Attribute = null,
            ),
        )
        val buildTypes = listOf("release", "debug")
        testCases.forEach { testCase ->
            buildTypes.forEach { buildType ->
                val mainFrameworkConfiguration = frameworkProducer.configurations.getByName("main${buildType.capitalize()}Framework${testCase.target.capitalize()}")
                mainFrameworkConfiguration.validateOutgoing(
                    OutgoingArtifactCheck(
                        buildType = buildType.toUpperCase(),
                        frameworkTargets = setOf(testCase.targetAttribute),
                        frameworkName = "main.framework",
                        disambiguation1Attribute = testCase.expectedDisambiguation1Attribute,
                        disambiguation2Attribute = testCase.expectedDisambiguation2Attribute,
                    )
                )
            }
        }

        val customFrameworkConfiguration = frameworkProducer.configurations.getByName("customReleaseFrameworkIosArm64")
        customFrameworkConfiguration.validateOutgoing(
            OutgoingArtifactCheck(
                buildType = "RELEASE",
                frameworkTargets = setOf("ios_arm64"),
                frameworkName = "custom.framework",
                disambiguation1Attribute = "someValue",
                disambiguation2Attribute = "someValue2",
            )
        )
    }

    @Test
    fun `universal framework configurations - output a single artifact with underlying targets as attributes`() {
        val frameworkProducer = buildProjectWithMPP {
            kotlin {
                iosArm64 {
                    // Applying attributes at either target or framework level doesn't affect outgoing universal framework configuration
                    attributes.setAttribute(disambiguation1Attribute, "someValue")
                    binaries.framework("main") {
                        attributes.setAttribute(disambiguation2Attribute, "someValue2")
                    }
                }
                iosX64 {
                    binaries.framework("main")
                }
            }
        }.evaluate()

        val buildTypes = listOf("release", "debug")
        buildTypes.forEach { buildType ->
            val universalFrameworkConfiguration = frameworkProducer.configurations.getByName("main${buildType.capitalize()}FrameworkIosFat")
            universalFrameworkConfiguration.validateOutgoing(
                OutgoingArtifactCheck(
                    buildType = buildType.toUpperCase(),
                    frameworkTargets = setOf("ios_x64", "ios_arm64"),
                    frameworkName = "main.framework",
                    disambiguation1Attribute = null,
                    disambiguation2Attribute = null,
                )
            )
        }
    }

    private data class OutgoingArtifactCheck(
        val frameworkName: String,
        val frameworkTargets: Set<String>?,
        val buildType: String?,
        val disambiguation1Attribute: String?,
        val disambiguation2Attribute: String?,
    )

    private val disambiguation1Attribute = Attribute.of("myDisambiguation1Attribute", String::class.java)
    private val disambiguation2Attribute = Attribute.of("myDisambiguation2Attribute", String::class.java)
    private val frameworkTargets = Attribute.of(
        "org.jetbrains.kotlin.native.framework.targets",
        Set::class.java
    )
    private val kotlinNativeBuildTypeAttribute = Attribute.of(
        "org.jetbrains.kotlin.native.build.type",
        String::class.java
    )

    private fun Configuration.validateOutgoing(
        expectedArtifact: OutgoingArtifactCheck,
    ) {
        assertEquals(
            listOf(expectedArtifact),
            outgoing.artifacts.map { framework ->
                OutgoingArtifactCheck(
                    frameworkName = framework.file.name,
                    frameworkTargets = outgoing.attributes.getAttribute(frameworkTargets)?.assertedCast<Set<String>> { "Couldn't cast framework targets" },
                    buildType = outgoing.attributes.getAttribute(kotlinNativeBuildTypeAttribute)?.assertedCast<String> { "Couldn't cast built type" },
                    disambiguation1Attribute = outgoing.attributes.getAttribute(disambiguation1Attribute)?.assertedCast<String> { "Couldn't cast disambiguation1Attribute" },
                    disambiguation2Attribute = outgoing.attributes.getAttribute(disambiguation2Attribute)?.assertedCast<String> { "Couldn't cast disambiguation2Attribute" },
                )
            },
            "Configuration ${name} has an unexpected outgoing artifact"
        )
    }
}