/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.BuildActions
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.collectFusEvents
import org.jetbrains.kotlin.gradle.testbase.compileStubSourceWithSourceSetName
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.createDirectories
import kotlin.test.assertEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class SwiftPMImportFUSIT : KGPBaseTest() {

    @GradleTest
    fun `swiftPM direct dependencies FUS - reports boolean and sum of numeric cound of consumed dependencies`(version: GradleVersion) {
        val booleanFusEventName = BooleanMetrics.KMP_SWIFT_PM_IMPORT_HAS_DIRECT_DEPENDENCIES.name
        val numericFusEventName = NumericalMetrics.KMP_SWIFT_PM_IMPORT_NUMBER_OF_DIRECT_DEPENDENCIES.name
        project("empty", version) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosSimulatorArm64()
                    iosArm64()
                }
            }
            assertEquals(
                emptyList(),
                collectFusEvents(
                    ":help",
                    buildAction = BuildActions.build
                ).filter { event ->
                    listOf(booleanFusEventName, numericFusEventName).any { eventName -> event.startsWith(eventName) }
                },
            )

            buildScriptInjection {
                project.applyMultiplatform {
                    swiftPMDependencies {
                        swiftPackage(url = "https://foo.bar", version = "1.0.0", products = emptyList())
                    }
                }
            }

            assertEquals(
                setOf("${booleanFusEventName}=true", "${numericFusEventName}=2"),
                collectFusEvents(
                    ":help",
                    buildAction = BuildActions.build
                ).filter { event ->
                    listOf(booleanFusEventName, numericFusEventName).any { eventName -> event.startsWith(eventName) }
                }.toSet(),
            )

            include(
                project("empty", version) {
                    plugins {
                        kotlin("multiplatform")
                    }
                    buildScriptInjection {
                        project.applyMultiplatform {
                            iosSimulatorArm64()
                            iosArm64()
                            swiftPMDependencies {
                                swiftPackage(url = "https://foo.bar", version = "1.0.0", products = emptyList())
                            }
                        }
                    }
                },
                "subproject"
            )

            assertEquals(
                setOf("${booleanFusEventName}=true", "${numericFusEventName}=4"),
                collectFusEvents(
                    ":help",
                    buildAction = BuildActions.build
                ).filter { event ->
                    listOf(booleanFusEventName, numericFusEventName).any { eventName -> event.startsWith(eventName) }
                }.toSet(),
            )
        }
    }

    @GradleTest
    fun `swiftPM transitive dependencies FUS - reports transitive dependencies in the case of modular dependency`(version: GradleVersion) {
        val booleanFusEventName = BooleanMetrics.KMP_SWIFT_PM_IMPORT_HAS_TRANSITIVE_DEPENDENCIES_FROM_MODULAR_DEPENDENCIES.name
        project("empty", version) {
            include(
                project("empty", version) {
                    val packageDependency = projectPath.resolve("packageDependency").also { it.createDirectories() }.toFile()
                    runProcess(listOf("swift", "package", "init", "--type", "library"), packageDependency)

                    plugins {
                        kotlin("multiplatform")
                    }
                    buildScriptInjection {
                        project.applyMultiplatform {
                            iosSimulatorArm64()
                            iosArm64()
                            swiftPMDependencies {
                                localSwiftPackage(
                                    directory = project.layout.projectDirectory.dir("packageDependency"),
                                    products = listOf<String>(),
                                )
                            }
                        }
                    }
                },
                "subproject"
            )

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosSimulatorArm64()
                    iosArm64()
                    sourceSets.commonMain.get().dependencies { implementation(project(":subproject")) }
                }
            }

            // Project dependencies shouldn't trigger this FUS event
            assertEquals(
                listOf("${booleanFusEventName}=false"),
                collectFusEvents(
                    ":generateSyntheticLinkageSwiftPMImportProjectForCinteropsAndLdDump",
                    buildAction = BuildActions.build
                ).filter { event ->
                    listOf(booleanFusEventName).any { eventName -> event.startsWith(eventName) }
                },
            )

            val publishedDependency = project("empty", version) {
                val packageDependency = projectPath.resolve("packageDependency").also { it.createDirectories() }.toFile()
                runProcess(listOf("swift", "package", "init", "--type", "library"), packageDependency)

                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosSimulatorArm64()
                        iosArm64()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                        swiftPMDependencies {
                            localSwiftPackage(
                                directory = project.layout.projectDirectory.dir("packageDependency"),
                                products = listOf<String>(),
                            )
                        }
                    }
                }
            }.publish()

            addPublishedProjectToRepositories(publishedDependency)
            buildScriptInjection {
                project.applyMultiplatform {
                    sourceSets.commonMain.get().dependencies { implementation(publishedDependency.rootCoordinate) }
                }
            }
            // But modular dependencies should
            assertEquals(
                listOf("${booleanFusEventName}=true"),
                collectFusEvents(
                    ":generateSyntheticLinkageSwiftPMImportProjectForCinteropsAndLdDump",
                    buildAction = BuildActions.build
                ).filter { event ->
                    listOf(booleanFusEventName).any { eventName -> event.startsWith(eventName) }
                },
            )
        }
    }

    @GradleTest
    fun `cocoapods direct dependencies FUS - reports boolean and sum of numeric cound of consumed dependencies`(version: GradleVersion) {
        val booleanFusEventName = BooleanMetrics.KMP_COCOAPODS_HAS_DIRECT_DEPENDENCIES.name
        val numericFusEventName = NumericalMetrics.KMP_COCOAPODS_NUMBER_OF_DIRECT_DEPENDENCIES.name
        project("empty", version) {
            plugins {
                kotlin("multiplatform")
                kotlin("native.cocoapods")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosSimulatorArm64()
                    iosArm64()
                    cocoapods.version = "1.0"
                }
            }
            assertEquals(
                emptyList(),
                collectFusEvents(
                    ":help",
                    buildAction = BuildActions.build
                ).filter { event ->
                    listOf(booleanFusEventName, numericFusEventName).any { eventName -> event.startsWith(eventName) }
                },
            )

            buildScriptInjection {
                project.applyMultiplatform {
                    cocoapods.pod("foo")
                }
            }

            assertEquals(
                setOf("${booleanFusEventName}=true", "${numericFusEventName}=1"),
                collectFusEvents(
                    ":help",
                    buildAction = BuildActions.build
                ).filter { event ->
                    listOf(booleanFusEventName, numericFusEventName).any { eventName -> event.startsWith(eventName) }
                }.toSet(),
            )

            include(
                project("empty", version) {
                    plugins {
                        kotlin("multiplatform")
                        kotlin("native.cocoapods")
                    }
                    buildScriptInjection {
                        project.applyMultiplatform {
                            cocoapods.version = "1.0"
                            cocoapods.pod("bar")
                        }
                    }
                },
                "subproject"
            )

            assertEquals(
                setOf("${booleanFusEventName}=true", "${numericFusEventName}=2"),
                collectFusEvents(
                    ":help",
                    buildAction = BuildActions.build
                ).filter { event ->
                    listOf(booleanFusEventName, numericFusEventName).any { eventName -> event.startsWith(eventName) }
                }.toSet(),
            )
        }
    }
}
