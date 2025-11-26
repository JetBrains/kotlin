/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.junit.jupiter.api.condition.OS
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftImportExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.dumpKlibMetadata
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.test.assertEquals
import org.jetbrains.kotlin.gradle.util.isTeamCityRun
import org.junit.jupiter.api.Assumptions
import kotlin.collections.mapOf
import kotlin.time.measureTime

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@NativeGradlePluginTests
class SwiftPMImportPopularSwiftPMDependenciesTests : KGPBaseTest() {

    @GradleTest
    fun `direct dependency on Firebase`(version: GradleVersion) = testVisibleSignatures(
        version = version,
        expectedCinteropAPIs = mapOf(
            "firestoreForApp" to """
                      @kotlinx/cinterop/ObjCMethod(encoding = "@32@0:8@16@24", selector = "firestoreForApp:database:", isStret = false)
                      public open expect fun firestoreForApp(app: swiftPMImport/empty/FIRApp, database: kotlin/String): swiftPMImport/empty/FIRFirestore
                      @kotlinx/cinterop/ObjCMethod(encoding = "@24@0:8@16", selector = "firestoreForApp:", isStret = false)
                      public open expect fun firestoreForApp(app: swiftPMImport/empty/FIRApp): swiftPMImport/empty/FIRFirestore
                """.trimIndent(),
            "FIRAnalytics" to """
                        // class name: swiftPMImport/empty/FIRAnalytics.Companion
                        // class name: swiftPMImport/empty/FIRAnalytics
                        public final expect companion object swiftPMImport/empty/FIRAnalytics.Companion : swiftPMImport/empty/FIRAnalyticsMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/empty/FIRAnalytics> {
                        public open expect class swiftPMImport/empty/FIRAnalytics : platform/darwin/NSObject {
                          @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "FIRAnalytics()"))
                          public open expect fun init(): swiftPMImport/empty/FIRAnalytics?
                        // class name: swiftPMImport/empty/FIRAnalyticsMeta
                        public open expect class swiftPMImport/empty/FIRAnalyticsMeta : platform/darwin/NSObjectMeta {
                          public open expect fun new(): swiftPMImport/empty/FIRAnalytics?
                          public open expect fun alloc(): swiftPMImport/empty/FIRAnalytics?
                          public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/empty/FIRAnalytics?
                          public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.initiateOnDeviceConversionMeasurementWithEmailAddress(emailAddress: kotlin/String): kotlin/Unit
                          public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.setConsent(consentSettings: kotlin/collections/Map<kotlin/Any?, *>): kotlin/Unit
                          public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.initiateOnDeviceConversionMeasurementWithHashedEmailAddress(hashedEmailAddress: platform/Foundation/NSData): kotlin/Unit
                          public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.initiateOnDeviceConversionMeasurementWithHashedPhoneNumber(hashedPhoneNumber: platform/Foundation/NSData): kotlin/Unit
                          public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.handleUserActivity(userActivity: kotlin/Any): kotlin/Unit
                          public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.handleOpenURL(url: platform/Foundation/NSURL): kotlin/Unit
                          public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.handleEventsForBackgroundURLSession(identifier: kotlin/String, completionHandler: kotlin/Function0<kotlin/Unit>?): kotlin/Unit
                          public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.initiateOnDeviceConversionMeasurementWithPhoneNumber(phoneNumber: kotlin/String): kotlin/Unit
                """.trimIndent()
        )
    ) {
        `package`(
            url = url("https://github.com/firebase/firebase-ios-sdk.git"),
            version = exact("12.5.0"),
            products = listOf(product("FirebaseAnalytics"), product("FirebaseFirestore")),
        )
        `package`(
            url = url("https://github.com/apple/swift-protobuf.git"),
            version = exact("1.32.0"),
            products = listOf(),
        )
    }

    private fun testVisibleSignatures(
        version: GradleVersion,
        expectedCinteropAPIs: Map<String, String>,
        configure: SwiftImportExtension.() -> Unit,
    ) {
        if (!isTeamCityRun) {
            Assumptions.assumeTrue(version >= GradleVersion.version("8.0"))
        }
        project("empty", version) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()

                    swiftPMDependencies {
                        configure()
                    }
                }
            }

            build("commonizeCInterop")

            val commonizerResult = projectPath.resolve("build/classes/kotlin/commonizer/swiftPMImport")
                .listDirectoryEntries()
                .single { it.isDirectory() }
                .listDirectoryEntries()
                .single { it.isDirectory() }
                .listDirectoryEntries()
                .single { it.isDirectory() }

            val metadataDump = dumpKlibMetadata(
                commonizerResult.toFile()
            )

            val actualSignatures = mutableMapOf<String, MutableList<String>>()
            measureTime {
                metadataDump.lines().forEach { line ->
                    expectedCinteropAPIs.keys.forEach { key ->
                        if (key in line) {
                            actualSignatures.getOrPut(key) { mutableListOf() }.add(line)
                        }
                    }
                }
            }.also {
                println(it.inWholeMilliseconds)
            }

            assertEquals(
                expectedCinteropAPIs.prettyPrinted,
                actualSignatures.mapValues { it.value.joinToString("\n").trimIndent() }.prettyPrinted,
            )
        }
    }
}

internal fun KotlinMultiplatformExtension.swiftPMDependencies(configure: SwiftImportExtension.() -> Unit) {
    (this.extensions.getByName(SwiftImportExtension.EXTENSION_NAME) as SwiftImportExtension).configure()
}