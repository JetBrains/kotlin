/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExportDsl::class, ExperimentalSwiftExportDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.EmbedSwiftExportForXcodeTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportedModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportTask
import org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportConfigurationDsl
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.unitTests.utils.applyEmbedAndSignEnvironment
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.EMBED_SWIFT_EXPORT_TASK_NAME
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.exportDslProject
import org.jetbrains.kotlin.gradle.util.exportExtension
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.junit.jupiter.api.Assumptions
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

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

class ExportExtensionSwiftExportTests {
    @BeforeTest
    fun runOnMacOSOnly() {
        Assumptions.assumeTrue(HostManager.hostIsMac, "macOS host required for this test")
    }

    @Test
    fun `direct external api dependency exported fully`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()

                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = setOf(
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxKotlinxIoBytestring",
                artifactName = "kotlinx-io-bytestring-iosSimulatorArm64Main-0.7.0.klib",
                shouldBeFullyExported = true
            ),
        )

        assertSetsEqual(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `direct external implementation dependency exported transitively`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()

                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = setOf(
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxKotlinxIoBytestring",
                artifactName = "kotlinx-io-bytestring-iosSimulatorArm64Main-0.7.0.klib",
                shouldBeFullyExported = false
            ),
        )

        assertSetsEqual(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `direct project api dependency exported fully`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )
        val projectDependency = project.subProject("subproject") {
            iosSimulatorArm64()
        }
        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api(projectDependency)
                }
            }
        )

        project.evaluate()
        projectDependency.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = setOf(
            ExportedSwiftModuleForAssertion(
                moduleName = "Subproject",
                artifactName = "subproject",
                shouldBeFullyExported = true
            ),
        )

        assertSetsEqual(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `direct project implementation dependency exported transitively`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )
        val projectDependency = project.subProject("subproject") {
            iosSimulatorArm64()
        }
        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation(projectDependency)
                }
            }
        )

        project.evaluate()
        projectDependency.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = setOf(
            ExportedSwiftModuleForAssertion(
                moduleName = "SharedSubproject",
                artifactName = "subproject",
                shouldBeFullyExported = false
            ),
        )

        assertSetsEqual(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `direct project api dependency exported fully, its dependencies exported transitively`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )
        val projectDependency = project.subProject("subproject") {
            iosSimulatorArm64()
            sourceSets.commonMain.dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            }
        }
        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api(projectDependency)
                }
            }
        )

        project.evaluate()
        projectDependency.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = setOf(
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxAtomicfu",
                artifactName = "atomicfu.klib",
                shouldBeFullyExported = false
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                artifactName = "kotlinx-coroutines-core.klib",
                shouldBeFullyExported = false
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxKotlinxDatetime",
                artifactName = "kotlinx-datetime.klib",
                shouldBeFullyExported = false
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxKotlinxSerializationCore",
                artifactName = "kotlinx-serialization-core.klib",
                shouldBeFullyExported = false
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "Subproject",
                artifactName = "subproject",
                shouldBeFullyExported = true
            ),
        )

        assertSetsEqual(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `jvm dependency is not exported`() {
        val project = swiftExportProject(
            projectBuilder = {
                withName("shared")
            },
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.glassfish:jakarta.json:2.0.1")
                }
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        assertTrue(actualModules.isEmpty(), "No modules should be exported for JVM dependencies")
    }

    @Test
    fun `exporting transitive dependencies with different versions (dependency in subproject has greater version)`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )
        val projectDependency = project.subProject("subproject") {
            iosSimulatorArm64()
            sourceSets.commonMain.dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
            }
        }
        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation(projectDependency)
                    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                }
            }
        )

        project.evaluate()
        projectDependency.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = setOf(
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxAtomicfu",
                artifactName = "atomicfu.klib",
                shouldBeFullyExported = false
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                artifactName = "kotlinx-coroutines-core-iosSimulatorArm64Main-1.10.0.klib",
                shouldBeFullyExported = true
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "SharedSubproject",
                artifactName = "subproject",
                shouldBeFullyExported = false
            ),
        )

        assertSetsEqual(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `exporting transitive dependencies with different versions (dependency in subproject has lower version)`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )
        val projectDependency = project.subProject("subproject") {
            iosSimulatorArm64()
            sourceSets.commonMain.dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }
        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation(projectDependency)
                    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
                }
            }
        )

        project.evaluate()
        projectDependency.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = setOf(
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxAtomicfu",
                artifactName = "atomicfu.klib",
                shouldBeFullyExported = false
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                artifactName = "kotlinx-coroutines-core-iosSimulatorArm64Main-1.10.0.klib",
                shouldBeFullyExported = true
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "SharedSubproject",
                artifactName = "subproject",
                shouldBeFullyExported = false
            ),
        )

        assertSetsEqual(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `exporting two runtime modules`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )

        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("app.cash.sqldelight:runtime:2.1.0")
                    api("org.jetbrains.compose.runtime:runtime:1.8.2")
                }
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = setOf(
            ExportedSwiftModuleForAssertion(
                moduleName = "AppCashSqldelightRuntime",
                artifactName = "runtime.klib",
                shouldBeFullyExported = true
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsComposeRuntimeRuntime",
                artifactName = "runtime-uikitSimArm64Main-1.8.2.klib",
                shouldBeFullyExported = true
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxAtomicfu",
                artifactName = "atomicfu.klib",
                shouldBeFullyExported = false
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                artifactName = "kotlinx-coroutines-core.klib",
                shouldBeFullyExported = false
            ),
        )

        assertSetsEqual(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `excluded transitive dependencies not exported`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )
        val projectDependency = project.subProject("subproject") {
            iosSimulatorArm64()
            sourceSets.commonMain.dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2") {
                    exclude(mapOf("group" to "org.jetbrains.kotlinx", "module" to "kotlinx-serialization-core"))
                }
            }
        }
        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api(projectDependency)
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0") {
                        exclude(mapOf("group" to "org.jetbrains.kotlinx", "module" to "atomicfu"))
                    }
                }
            }
        )

        project.evaluate()
        projectDependency.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = setOf(
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                artifactName = "kotlinx-coroutines-core.klib",
                shouldBeFullyExported = false
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "OrgJetbrainsKotlinxKotlinxDatetime",
                artifactName = "kotlinx-datetime.klib",
                shouldBeFullyExported = false
            ),
            ExportedSwiftModuleForAssertion(
                moduleName = "Subproject",
                artifactName = "subproject",
                shouldBeFullyExported = true
            ),
        )

        assertSetsEqual(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }
}

private fun swiftExportProject(
    configuration: String = "DEBUG",
    sdk: String = "iphonesimulator",
    archs: String = "arm64",
    projectBuilder: ProjectBuilder.() -> Unit = { },
    multiplatform: KotlinMultiplatformExtension.() -> Unit = {
        iosSimulatorArm64()
    },
    swiftExport: SwiftExportConfigurationDsl.() -> Unit = {},
): ProjectInternal = buildProjectWithMPP(
    projectBuilder = projectBuilder,
    preApplyCode = {
        applyEmbedAndSignEnvironment(
            configuration = configuration,
            sdk = sdk,
            archs = archs,
        )
        configureRepositoriesForTests()
    },
    code = {
        kotlin {
            multiplatform()
        }
        exportExtension.swift {
            xcodeIntegration()
            swiftExport()
        }
    }
)

private fun ProjectInternal.setupForSwiftExport(
    configuration: String = "DEBUG",
    sdk: String = "iphonesimulator",
    archs: String = "arm64",
    multiplatform: KotlinMultiplatformExtension.() -> Unit = {
        iosSimulatorArm64()
    },
    swiftExport: SwiftExportConfigurationDsl.() -> Unit = {},
) {
    applyEmbedAndSignEnvironment(
        configuration = configuration,
        sdk = sdk,
        archs = archs,
    )
    applyMultiplatformPlugin()
    kotlin {
        multiplatform()
    }
    exportExtension.swift {
        xcodeIntegration()
        swiftExport()
    }
}

private fun ProjectInternal.subProject(
    name: String,
    multiplatform: KotlinMultiplatformExtension.() -> Unit = { iosSimulatorArm64() },
): ProjectInternal = buildProjectWithMPP(
    projectBuilder = {
        withParent(this@subProject)
        withName(name)
    },
    code = {
        kotlin {
            multiplatform()
        }
    }
)

/**
 * Asserts that two sets are equal, but renders each set as a vertical, alphabetically sorted list of the string
 * representations of its elements. This makes the failure message much easier to eyeball and diff than the default
 * [Set.toString], because both sets are presented line-by-line in the same order.
 */
private fun <T> assertSetsEqual(expected: Set<T>, actual: Set<T>, message: String? = null) {
    fun Set<T>.renderAsSortedLines() = map { it.toString() }.sorted().joinToString(separator = "\n")
    assertEquals(expected.renderAsSortedLines(), actual.renderAsSortedLines(), message)
}

private fun List<SwiftExportedModule>.toModulesForAssertion() = mapToSetOrEmpty { module ->
    ExportedSwiftModuleForAssertion(
        module.moduleName,
        module.artifact.name,
        module.shouldBeFullyExported
    )
}

private data class ExportedSwiftModuleForAssertion(
    val moduleName: String,
    val artifactName: String,
    val shouldBeFullyExported: Boolean,
)
