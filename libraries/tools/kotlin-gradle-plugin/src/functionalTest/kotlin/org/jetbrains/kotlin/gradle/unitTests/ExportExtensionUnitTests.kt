/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExportDsl::class, ExperimentalSwiftExportDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.export.EXPORT_EXTENSION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.export.ExportExtension
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class ExportExtensionUnitTests {

    @Test
    fun `test export extension is registered on the multiplatform extension`() {
        val project = buildProjectWithMPP()
        assertNotNull(project.exportExtension)
    }

    @Test
    fun `test swift export configuration is readable from the dsl`() {
        val project = buildProjectWithMPP()
        project.exportExtension.swift {
            moduleName.set("Shared")
            rootPackage.set("com.example.shared")
        }

        val configuration = project.exportExtension.swiftExportConfiguration
        assertEquals("Shared", configuration.moduleName.get())
        assertEquals("com.example.shared", configuration.rootPackage.get())
    }

    @Test
    fun `test xcode integration is not activated without an explicit call`() {
        val project = buildProjectWithMPP()
        project.exportExtension.swift {
            moduleName.set("Shared")
        }

        assertNull(project.exportExtension.swiftExportConfiguration.activatedXcodeIntegration)
    }

    @Test
    fun `test xcode integration is activated without a configuration block`() {
        val project = buildProjectWithMPP()
        project.exportExtension.swift {
            xcodeIntegration()
        }

        val integration = assertNotNull(project.exportExtension.swiftExportConfiguration.activatedXcodeIntegration)
        assertEquals(emptyMap(), integration.settings.get())
    }

    @Test
    fun `test xcode integration settings are readable from the configuration`() {
        val project = buildProjectWithMPP()
        project.exportExtension.swift {
            xcodeIntegration {
                settings.put("key", "value")
            }
        }

        val integration = assertNotNull(project.exportExtension.swiftExportConfiguration.activatedXcodeIntegration)
        assertEquals(mapOf("key" to "value"), integration.settings.get())
    }

    @Test
    fun `test repeated xcode integration calls configure the same integration`() {
        val project = buildProjectWithMPP()
        project.exportExtension.swift {
            xcodeIntegration {
                settings.put("first", "1")
            }
        }

        val integration = assertNotNull(project.exportExtension.swiftExportConfiguration.activatedXcodeIntegration)

        project.exportExtension.swift {
            xcodeIntegration()
            xcodeIntegration {
                settings.put("second", "2")
            }
        }

        assertSame(integration, project.exportExtension.swiftExportConfiguration.activatedXcodeIntegration)
        assertEquals(mapOf("first" to "1", "second" to "2"), integration.settings.get())
    }

    private val Project.exportExtension: ExportExtension
        get() = assertNotNull(
            multiplatformExtension.getExtension(EXPORT_EXTENSION_NAME),
            "Expected the `$EXPORT_EXTENSION_NAME` extension to be registered on the multiplatform extension"
        )
}
