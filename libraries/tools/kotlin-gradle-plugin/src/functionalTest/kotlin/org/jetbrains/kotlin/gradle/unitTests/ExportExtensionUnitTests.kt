/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExportDsl::class, ExperimentalSwiftExportDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.EmbedSwiftExportForXcodeTask
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.util.EMBED_SWIFT_EXPORT_TASK_NAME
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.exportDslProject
import org.jetbrains.kotlin.gradle.util.exportExtension
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.Assumptions
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
}

class ExportExtensionXcodeIntegrationTests {

    @BeforeTest
    fun runOnMacOSOnly() {
        Assumptions.assumeTrue(HostManager.hostIsMac, "macOS host required for this test")
    }

    @Test
    fun `test embed task is registered when the xcode integration is activated`() {
        val project = exportDslProject {
            exportExtension.swift {
                moduleName.set("Shared")
                xcodeIntegration()
            }
        }

        assertNotNull(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
    }

    @Test
    fun `test embed task is not registered when the export dsl is used without the xcode integration`() {
        val project = exportDslProject {
            exportExtension.swift {
                moduleName.set("Shared")
            }
        }

        assertNull(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
    }

    @Test
    fun `test embed task is registered when the export dsl is not used at all`() {
        val project = exportDslProject()

        assertNotNull(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
    }

    @Test
    fun `test the xcode integration can be activated after the module is configured`() {
        val project = exportDslProject {
            exportExtension.swift {
                moduleName.set("Shared")
            }
            exportExtension.swift {
                xcodeIntegration()
            }
        }

        assertNotNull(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
    }

    @Test
    fun `test the pipeline is registered for an activated project in the xcode environment`() {
        val project = exportDslProject(withXcodeEnvironment = true) {
            exportExtension.swift {
                xcodeIntegration()
            }
        }

        assertIs<EmbedSwiftExportForXcodeTask>(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
        assertNotNull(project.tasks.findByName("iosSimulatorArm64DebugSwiftExport"))
    }

    @Test
    fun `test the pipeline is not registered for a project that opted out of the xcode integration`() {
        val project = exportDslProject(withXcodeEnvironment = true) {
            exportExtension.swift {
                moduleName.set("Shared")
            }
        }

        assertNull(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
        assertNull(project.tasks.findByName("iosSimulatorArm64DebugSwiftExport"))
    }

    @Test
    fun `test embed task is registered when the legacy swift export dsl is used`() {
        val project = exportDslProject {
            kotlin {
                swiftExport {
                    moduleName.set("Legacy")
                }
            }
        }

        assertNotNull(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
    }

    @Test
    fun `test the export dsl takes precedence over the legacy swift export dsl`() {
        val project = exportDslProject {
            kotlin {
                swiftExport {
                    moduleName.set("Legacy")
                }
            }
            exportExtension.swift {
                moduleName.set("Shared")
            }
        }

        assertNull(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
    }
}
