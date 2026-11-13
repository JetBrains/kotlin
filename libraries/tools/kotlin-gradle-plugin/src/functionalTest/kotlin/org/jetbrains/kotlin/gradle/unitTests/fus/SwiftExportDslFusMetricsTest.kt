/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.fus

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportVisibility
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.exportExtension
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.StringListMetrics
import org.junit.jupiter.api.Assumptions
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SwiftExportDslFusMetricsTest {

    @BeforeTest
    fun runOnMacOSOnly() {
        Assumptions.assumeTrue(HostManager.hostIsMac, "macOS host required for this test")
    }

    @Test
    fun `nothing is reported when the export dsl is not used`() {
        val metrics = swiftExportDslMetrics()

        assertEquals(SwiftExportDslMetricValues(), metrics)
    }

    @Test
    fun `an empty swift block is reported as configured`() {
        val metrics = swiftExportDslMetrics {
            exportExtension.swift { }
        }

        assertEquals(SwiftExportDslMetricValues(configured = true), metrics)
    }

    @Test
    fun `overridden module options are reported`() {
        val metrics = swiftExportDslMetrics {
            exportExtension.swift {
                moduleName.set("Shared")
                rootPackage.set("com.example")
            }
        }

        assertEquals(
            SwiftExportDslMetricValues(configured = true, moduleOptionsOverrides = listOf("moduleName", "rootPackage")),
            metrics,
        )
    }

    @Test
    fun `xcode integration activation is reported`() {
        val metrics = swiftExportDslMetrics {
            exportExtension.swift {
                moduleName.set("Shared")
                xcodeIntegration()
            }
        }

        assertEquals(
            SwiftExportDslMetricValues(
                configured = true,
                moduleOptionsOverrides = listOf("moduleName"),
                xcodeIntegrationActivated = true,
            ),
            metrics,
        )
    }

    @Test
    fun `overridden xcode integration options are reported`() {
        val metrics = swiftExportDslMetrics {
            exportExtension.swift {
                moduleName.set("Shared")
                xcodeIntegration {
                    settings.put("key", "value")
                    configure("com.example:sub:1.0") {
                        moduleName.set("Sub")
                        rootPackage.set("com.example.sub")
                        visibility.set(SwiftExportVisibility.EXPOSED)
                    }
                    configure("com.example:other:1.0") {
                        visibility.set(SwiftExportVisibility.HIDDEN)
                    }
                }
            }
        }

        assertEquals(
            SwiftExportDslMetricValues(
                configured = true,
                moduleOptionsOverrides = listOf("moduleName"),
                xcodeIntegrationActivated = true,
                xcodeIntegrationOverrides = listOf("exposed", "hidden", "moduleName", "rootPackage", "settings"),
            ),
            metrics,
        )
    }

    private data class SwiftExportDslMetricValues(
        val configured: Boolean? = null,
        val moduleOptionsOverrides: List<String>? = null,
        val xcodeIntegrationActivated: Boolean? = null,
        val xcodeIntegrationOverrides: List<String>? = null,
    )

    private fun swiftExportDslMetrics(configureExport: Project.() -> Unit = {}): SwiftExportDslMetricValues {
        val project = buildProjectWithMPP(
            preApplyCode = {
                enableFusOnCI()
                configureRepositoriesForTests()
            },
            code = {
                kotlin { iosArm64() }
                configureExport()
            }
        )
        project.evaluate()

        val metrics = project.collectedFusConfigurationTimeMetrics
        return SwiftExportDslMetricValues(
            configured = metrics.booleanMetrics[BooleanMetrics.SWIFT_EXPORT_DSL_CONFIGURED],
            moduleOptionsOverrides = metrics.stringListMetrics[StringListMetrics.SWIFT_EXPORT_DSL_MODULE_OPTIONS_OVERRIDES],
            xcodeIntegrationActivated = metrics.booleanMetrics[BooleanMetrics.SWIFT_EXPORT_DSL_XCODE_INTEGRATION_ACTIVATED],
            xcodeIntegrationOverrides = metrics.stringListMetrics[StringListMetrics.SWIFT_EXPORT_DSL_XCODE_INTEGRATION_OVERRIDES],
        )
    }
}
