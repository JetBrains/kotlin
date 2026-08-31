/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExportDsl::class, ExperimentalSwiftExportDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.export.EXPORT_EXTENSION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.export.ExportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.EmbedSwiftExportForXcodeTask
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.unitTests.utils.applyEmbedAndSignEnvironment
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
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

    private val Project.exportExtension: ExportExtension
        get() = assertNotNull(
            multiplatformExtension.getExtension(EXPORT_EXTENSION_NAME),
            "Expected the `$EXPORT_EXTENSION_NAME` extension to be registered on the multiplatform extension"
        )
}

class ExportExtensionXcodeIntegrationTests {

    @BeforeTest
    fun runOnMacOSOnly() {
        Assumptions.assumeTrue(HostManager.hostIsMac, "macOS host required for this test")
    }

    @Test
    fun `test embed task is registered when the xcode integration is activated`() {
        val project = appleProject {
            exportExtension.swift {
                moduleName.set("Shared")
                xcodeIntegration()
            }
        }

        assertNotNull(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
    }

    @Test
    fun `test embed task is not registered when the export dsl is used without the xcode integration`() {
        val project = appleProject {
            exportExtension.swift {
                moduleName.set("Shared")
            }
        }

        assertNull(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
    }

    @Test
    fun `test embed task is registered when the export dsl is not used at all`() {
        val project = appleProject()

        assertNotNull(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
    }

    @Test
    fun `test the xcode integration can be activated after the module is configured`() {
        val project = appleProject {
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
        val project = appleProject(withXcodeEnvironment = true) {
            exportExtension.swift {
                xcodeIntegration()
            }
        }

        assertIs<EmbedSwiftExportForXcodeTask>(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
        assertNotNull(project.tasks.findByName("iosSimulatorArm64DebugSwiftExport"))
    }

    @Test
    fun `test the pipeline is not registered for a project that opted out of the xcode integration`() {
        val project = appleProject(withXcodeEnvironment = true) {
            exportExtension.swift {
                moduleName.set("Shared")
            }
        }

        assertNull(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
        assertNull(project.tasks.findByName("iosSimulatorArm64DebugSwiftExport"))
    }

    @Test
    fun `test embed task is registered when the legacy swift export dsl is used`() {
        val project = appleProject {
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
        val project = appleProject {
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

    private fun appleProject(
        withXcodeEnvironment: Boolean = false,
        multiplatform: KotlinMultiplatformExtension.() -> Unit = { iosSimulatorArm64() },
        configureExport: Project.() -> Unit = {},
    ): ProjectInternal = buildProjectWithMPP(
        preApplyCode = {
            if (withXcodeEnvironment) {
                applyEmbedAndSignEnvironment(configuration = "DEBUG", sdk = "iphonesimulator", archs = "arm64")
            }
            configureRepositoriesForTests()
        },
        code = {
            kotlin { multiplatform() }
            configureExport()
        }
    ).also { it.evaluate() }

    private val Project.exportExtension: ExportExtension
        get() = assertNotNull(multiplatformExtension.getExtension(EXPORT_EXTENSION_NAME))

    private companion object {
        const val EMBED_SWIFT_EXPORT_TASK_NAME = "embedSwiftExportForXcode"
    }
}
