/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.*
import org.jetbrains.kotlin.gradle.util.assertSwiftExportMetadataVariantExistsInRootComponent
import org.jetbrains.kotlin.gradle.util.assertSwiftExportMetadataVariantMissingInRootComponent
import org.jetbrains.kotlin.gradle.util.parseJsonToMap
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Serializing Swift Export metadata doesn't need Xcode, so unlike [SwiftExportDslIT] these tests run on any host.
 */
@DisplayName("Tests for Swift Export metadata publication with the export DSL")
@SwiftExportGradlePluginTests
@OptIn(ExperimentalExportDsl::class, ExperimentalSwiftExportDsl::class)
class SwiftExportMetadataPublishingIT : KGPBaseTest() {

    @DisplayName("swiftExport metadata is published into the root component when module name and root package are defined")
    @GradleTest
    fun testSwiftExportMetadataPublication(
        gradleVersion: GradleVersion,
    ) {
        val publishedProject = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                export.swift {
                    moduleName.set("Foo")
                    rootPackage.set("org.bar.foo")
                }
            }
        }.publish()

        val metadataFile = publishedProject.rootComponent.swiftExportMetadata
        assertFileExists(metadataFile.toPath())

        val metadata = parseJsonToMap(metadataFile.toPath())
        assertEquals(1, metadata["schemaVersion"]?.jsonPrimitive?.int)
        assertEquals("Foo", metadata["moduleName"]?.jsonPrimitive?.content)
        assertEquals("org.bar.foo", metadata["rootPackage"]?.jsonPrimitive?.content)

        publishedProject.assertSwiftExportMetadataVariantExistsInRootComponent()
    }

    @DisplayName("swiftExport metadata omits the module name when only the root package is defined")
    @GradleTest
    fun testSwiftExportMetadataWithoutModuleName(
        gradleVersion: GradleVersion,
    ) {
        val publishedProject = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                export.swift {
                    rootPackage.set("org.bar.foo")
                }
            }
        }.publish()

        val metadata = parseJsonToMap(publishedProject.rootComponent.swiftExportMetadata.toPath())
        assertNull(metadata["moduleName"])
        assertEquals("org.bar.foo", metadata["rootPackage"]?.jsonPrimitive?.content)
    }

    @DisplayName("swiftExport metadata is not published when neither module name nor root package is defined")
    @GradleTest
    fun testSwiftExportMetadataIsNotPublishedWithoutConfiguration(
        gradleVersion: GradleVersion,
    ) {
        val publishedProject = project("empty", gradleVersion) {
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

        assertFileNotExists(publishedProject.rootComponent.swiftExportMetadata.toPath())
        publishedProject.assertSwiftExportMetadataVariantMissingInRootComponent()
    }

    @DisplayName("swiftExport metadata is not published when the project has no apple targets")
    @GradleTest
    fun testSwiftExportMetadataIsNotPublishedWithoutAppleTargets(
        gradleVersion: GradleVersion,
    ) {
        val publishedProject = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                export.swift {
                    moduleName.set("Foo")
                    rootPackage.set("org.bar.foo")
                }
            }
        }.publish()

        assertFileNotExists(publishedProject.rootComponent.swiftExportMetadata.toPath())
        publishedProject.assertSwiftExportMetadataVariantMissingInRootComponent()
    }
}
