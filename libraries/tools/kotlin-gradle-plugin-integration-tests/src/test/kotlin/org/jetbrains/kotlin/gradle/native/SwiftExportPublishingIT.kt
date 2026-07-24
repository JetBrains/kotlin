/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.jetbrains.kotlin.gradle.util.assertSwiftExportMetadataVariantExistsInRootComponent
import org.jetbrains.kotlin.gradle.util.parseJsonToMap
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Serializing Swift Export metadata doesn't need Xcode, so unlike [SwiftExportDslIT] these tests run on any host.
 */
@DisplayName("Tests for Swift Export metadata publication")
@SwiftExportGradlePluginTests
class SwiftExportPublishingIT : KGPBaseTest() {

    @DisplayName("swiftExport metadata is published into the root component when module name and package flatten rule are defined")
    @GradleTest
    fun testSwiftExportMetadataPublication(
        gradleVersion: GradleVersion,
    ) {
        val publishedProject = project(
            "empty",
            gradleVersion,
        ) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    with(swiftExport) {
                        moduleName.set("Foo")
                        flattenPackage.set("org.bar.foo")
                    }
                }
            }
        }.publish()

        val metadataFile = publishedProject.rootComponent.swiftExportMetadata
        assertFileExists(metadataFile.toPath())

        val metadata = parseJsonToMap(metadataFile.toPath())
        assertEquals(1, metadata["schemaVersion"]?.jsonPrimitive?.int)
        assertEquals("Foo", metadata["moduleName"]?.jsonPrimitive?.content)
        assertEquals("org.bar.foo", metadata["flattenPackage"]?.jsonPrimitive?.content)

        publishedProject.assertSwiftExportMetadataVariantExistsInRootComponent()
    }

    @DisplayName("swiftExport metadata is not published when neither module name nor package flatten rule is defined")
    @GradleTest
    fun testSwiftExportMetadataIsNotPublishedWithoutConfiguration(
        gradleVersion: GradleVersion,
    ) {
        val publishedProject = project(
            "empty",
            gradleVersion,
        ) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }
        }.publish()

        assertFalse(
            publishedProject.rootComponent.swiftExportMetadata.exists(),
            "Swift Export metadata should not be published when moduleName and flattenPackage are not configured"
        )
    }

    @DisplayName("swiftExport metadata is not published when the project has no apple targets")
    @GradleTest
    fun testSwiftExportMetadataIsNotPublishedWithoutAppleTargets(
        gradleVersion: GradleVersion,
    ) {
        val publishedProject = project(
            "empty",
            gradleVersion,
        ) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    with(swiftExport) {
                        moduleName.set("Foo")
                        flattenPackage.set("org.bar.foo")
                    }
                }
            }
        }.publish()

        assertFalse(
            publishedProject.rootComponent.swiftExportMetadata.exists(),
            "Swift Export only works for apple targets, so its metadata should not be published without them"
        )
    }
}
