/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.archive

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.assertNoDiagnostic
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.jetbrains.kotlin.gradle.util.swiftExportEmbedAndSignEnvVariables
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertNotNull

@SwiftExportGradlePluginTests
@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Swift Export of a project published in the Kotlin Archive format")
class KotlinArchiveSwiftExportConsumptionIT : KGPBaseTest() {

    @GradleTest
    @DisplayName("Swift Export of a consumer exports a library in the Kotlin Archive format")
    @OptIn(ExperimentalSwiftExportDsl::class)
    fun consumptionWithSwiftExportTest(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        val publishedProject = kotlinArchiveProducer(gradleVersion)
            .publish(publisherConfiguration = PublisherConfiguration(group = TEST_GROUP))

        project("empty", gradleVersion) {
            plugins { kotlin("multiplatform") }
            addPublishedProjectToRepositories(publishedProject)
            settingsBuildScriptInjection {
                settings.rootProject.name = "consumer"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    export.swift { xcodeIntegration { } }
                    sourceSets.commonMain {
                        compileSource(
                            """
                            class ConsumerClass {
                                fun callProducer() {
                                    commonMain()
                                }
                            }
                            """.trimIndent()
                        )
                        dependencies {
                            api(publishedProject.rootCoordinate)
                        }
                    }
                }
            }

            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
            ) {
                assertNoDiagnostic(KotlinToolingDiagnostics.UnsupportedKotlinArchiveUsage)

                val buildProductsDir = this@project.gradleRunner.environment?.get("BUILT_PRODUCTS_DIR")?.let { File(it) }
                assertNotNull(buildProductsDir)
                assertDirectoriesExist(
                    buildProductsDir.resolve("Consumer.swiftmodule").toPath(),
                    buildProductsDir.resolve("SharedBridge_Consumer").toPath(),
                    buildProductsDir.resolve("$PRODUCER_SWIFT_MODULE.swiftmodule").toPath(),
                ) { defaultMessage ->
                    defaultMessage + "\nContents of dir:\n" + buildProductsDir.list()?.joinToString(separator = "\n")
                }
            }
        }
    }

    companion object {
        private const val TEST_GROUP = "kotlinArchiveSwiftExportTest"
        private const val PRODUCER_SWIFT_MODULE = "KotlinArchiveSwiftExportTestProducer"
    }
}
