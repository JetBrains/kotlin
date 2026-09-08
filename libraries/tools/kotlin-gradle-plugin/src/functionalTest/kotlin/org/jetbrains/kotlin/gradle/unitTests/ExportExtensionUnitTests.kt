/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExportDsl::class, ExperimentalSwiftExportDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.provider.ProviderConvertible
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnosticFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.EmbedSwiftExportForXcodeTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportedModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportTask
import org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportConfigurationDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportDeclaredModuleOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportDependencySelector
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.export.tasks.SerializeSwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.export.tasks.locateOrRegisterSwiftExportMetadataTaskAndConsumableConfiguration
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.unitTests.utils.applyEmbedAndSignEnvironment
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.EMBED_SWIFT_EXPORT_TASK_NAME
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.exportDslProject
import org.jetbrains.kotlin.gradle.util.exportExtension
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.legacySwiftExportExtension
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.junit.jupiter.api.Assumptions
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.fail
import kotlin.test.assertFalse
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

    @Test
    fun `dependency overrides are readable from the dsl`() {
        val project = buildProjectWithMPP()
        project.exportExtension.swift {
            xcodeIntegration {
                configure("org.example:foo:1.0") {
                    moduleName.set("FooBar")
                    rootPackage.set("org.example.foo")
                }
            }
        }

        val integration = assertNotNull(project.exportExtension.swiftExportConfiguration.activatedXcodeIntegration)
        assertEquals(
            mapOf<SwiftExportDependencySelector, SwiftExportDeclaredModuleOptions>(
                SwiftExportDependencySelector.Module("org.example", "foo") to
                        SwiftExportDeclaredModuleOptions(moduleName = "FooBar", rootPackage = "org.example.foo")
            ),
            integration.dependencyOverrides.get(),
        )
    }

    @Test
    fun `repeated configure calls follow normal gradle property semantics`() {
        val project = buildProjectWithMPP()
        project.exportExtension.swift {
            xcodeIntegration {
                configure("org.example:foo:1.0") {
                    moduleName.set("First")
                    rootPackage.set("org.example.first")
                }
                configure("org.example:foo:1.0") {
                    moduleName.set("Second")
                }
            }
        }

        val integration = assertNotNull(project.exportExtension.swiftExportConfiguration.activatedXcodeIntegration)
        assertEquals(
            mapOf<SwiftExportDependencySelector, SwiftExportDeclaredModuleOptions>(
                SwiftExportDependencySelector.Module("org.example", "foo") to
                        SwiftExportDeclaredModuleOptions(moduleName = "Second", rootPackage = "org.example.first")
            ),
            integration.dependencyOverrides.get(),
        )
    }

    @Test
    fun `different notations for the same component collapse into one override`() {
        val project = buildProjectWithMPP()
        project.exportExtension.swift {
            xcodeIntegration {
                configure("org.example:foo:1.0") { moduleName.set("FromCoordinates") }
                configure(project.provider { project.dependencies.create("org.example:foo:2.5") }) {
                    rootPackage.set("org.example.foo")
                }
            }
        }

        val integration = assertNotNull(project.exportExtension.swiftExportConfiguration.activatedXcodeIntegration)
        assertEquals(
            mapOf<SwiftExportDependencySelector, SwiftExportDeclaredModuleOptions>(
                SwiftExportDependencySelector.Module("org.example", "foo") to
                        SwiftExportDeclaredModuleOptions(moduleName = "FromCoordinates", rootPackage = "org.example.foo")
            ),
            integration.dependencyOverrides.get(),
        )
    }

    @Test
    fun `a project dependency override is keyed by project path`() {
        val root = buildProjectWithMPP()
        buildProjectWithMPP(projectBuilder = { withParent(root); withName("sub") })
        root.exportExtension.swift {
            xcodeIntegration {
                configure(root.dependencies.project(mapOf("path" to ":sub"))) { moduleName.set("Sub") }
            }
        }

        val integration = assertNotNull(root.exportExtension.swiftExportConfiguration.activatedXcodeIntegration)
        assertEquals(
            mapOf<SwiftExportDependencySelector, SwiftExportDeclaredModuleOptions>(
                SwiftExportDependencySelector.ProjectPath(":sub") to
                        SwiftExportDeclaredModuleOptions(moduleName = "Sub", rootPackage = null)
            ),
            integration.dependencyOverrides.get(),
        )
    }

    @Test
    fun `a notation that cannot be converted is rejected at the configure call site`() {
        val project = buildProjectWithMPP()

        assertFailsWith<InvalidUserDataException> {
            project.exportExtension.swift {
                xcodeIntegration {
                    configure(":no-group") { moduleName.set("NoGroup") }
                }
            }
        }
    }

    @Test
    fun `a provider notation is not realized while configuring`() {
        val project = buildProjectWithMPP()
        var realized = false
        project.exportExtension.swift {
            xcodeIntegration {
                configure(project.provider { realized = true; "org.example:foo:1.0" }) { moduleName.set("Foo") }
            }
        }

        assertFalse(realized)
        val integration = assertNotNull(project.exportExtension.swiftExportConfiguration.activatedXcodeIntegration)
        assertEquals(
            setOf(SwiftExportDependencySelector.Module("org.example", "foo")),
            integration.dependencyOverrides.get().keys,
        )
        assertTrue(realized)
    }

    @Test
    fun `a provider convertible notation is accepted and stays lazy`() {
        val project = buildProjectWithMPP()
        var realized = false
        // A version catalog alias with nested aliases is generated as a ProviderConvertible, not a Provider.
        val notation = ProviderConvertible {
            project.provider { realized = true; project.dependencies.create("org.example:foo:1.0") }
        }
        project.exportExtension.swift {
            xcodeIntegration {
                configure(notation) { moduleName.set("Foo") }
            }
        }

        assertFalse(realized)
        val integration = assertNotNull(project.exportExtension.swiftExportConfiguration.activatedXcodeIntegration)
        assertEquals(
            setOf(SwiftExportDependencySelector.Module("org.example", "foo")),
            integration.dependencyOverrides.get().keys,
        )
        assertTrue(realized)
    }

    @Test
    fun `dependency overrides are not published`() {
        val project = buildProjectWithMPP()
        project.exportExtension.swift {
            moduleName.set("Shared")
            xcodeIntegration {
                configure("org.example:foo:1.0") {
                    moduleName.set("FooBar")
                    rootPackage.set("org.example.foo")
                }
            }
        }

        project.locateOrRegisterSwiftExportMetadataTaskAndConsumableConfiguration(
            project.exportExtension.swiftExportConfiguration
        )

        val serializeTask = project.tasks.withType(SerializeSwiftExportMetadata::class.java).single()
        assertEquals(
            SwiftExportMetadata(moduleName = "Shared", rootPackage = null),
            serializeTask.swiftExportMetadata(),
        )
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
    @Suppress("DEPRECATION") // Exercises the deprecated legacy Swift Export DSL on purpose.
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
    @Suppress("DEPRECATION") // Exercises the deprecated legacy Swift Export DSL on purpose.
    fun `test configuring both dsls reports a conflict and keeps the export dsl precedence`() {
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

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.ConflictingSwiftExportDsls)
        assertNull(project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME))
    }
}

class LegacySwiftExportDslDiagnosticsTests {

    @Test
    fun `test no diagnostics are reported when neither dsl is used`() {
        val project = legacyDslProject { }

        project.assertNoDiagnostics(KotlinToolingDiagnostics.DeprecatedSwiftExportDsl)
        project.assertNoDiagnostics(KotlinToolingDiagnostics.ConflictingSwiftExportDsls)
    }

    @Test
    fun `test no diagnostics are reported when only the export dsl is used`() {
        val project = legacyDslProject {
            exportExtension.swift {
                moduleName.set("Shared")
            }
        }

        project.assertNoDiagnostics(KotlinToolingDiagnostics.DeprecatedSwiftExportDsl)
        project.assertNoDiagnostics(KotlinToolingDiagnostics.ConflictingSwiftExportDsls)
    }

    @Test
    fun `test the deprecation is reported when only the legacy dsl is used`() {
        val project = legacyDslProject {
            legacySwiftExportExtension.moduleName.set("Legacy")
        }

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.DeprecatedSwiftExportDsl)
        project.assertNoDiagnostics(KotlinToolingDiagnostics.ConflictingSwiftExportDsls)
    }

    @Test
    fun `test the conflict is reported when both dsls are used`() {
        val project = legacyDslProject {
            legacySwiftExportExtension.moduleName.set("Legacy")
            exportExtension.swift {
                moduleName.set("Shared")
            }
        }

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.ConflictingSwiftExportDsls)
        project.assertNoDiagnostics(KotlinToolingDiagnostics.DeprecatedSwiftExportDsl)
    }

    @Test
    fun `test the conflict is reported regardless of the dsl call order`() {
        val project = legacyDslProject {
            exportExtension.swift {
                xcodeIntegration()
            }
            legacySwiftExportExtension.moduleName.set("Legacy")
        }

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.ConflictingSwiftExportDsls)
        project.assertNoDiagnostics(KotlinToolingDiagnostics.DeprecatedSwiftExportDsl)
    }

    @Test
    fun `test the deprecation is reported when the legacy dsl is configured before the targets`() {
        val project = buildProjectWithMPP(
            code = {
                legacySwiftExportExtension.moduleName.set("Legacy")
                kotlin { jvm() }
            }
        ).also { it.evaluate() }

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.DeprecatedSwiftExportDsl)
        project.assertNoDiagnostics(KotlinToolingDiagnostics.ConflictingSwiftExportDsls)
    }

    @Test
    @Suppress("DEPRECATION") // Exercises the deprecated legacy Swift Export DSL on purpose.
    fun `test the deprecation is reported when the legacy dsl is used through the kotlin entry point`() {
        val project = legacyDslProject {
            kotlin {
                swiftExport()
            }
        }

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.DeprecatedSwiftExportDsl)
    }

    /**
     * JVM-only so these tests stay host-independent: the diagnostics only depend on which DSL was
     * configured, and the Xcode wiring itself is only exercised for Apple targets, which needs macOS.
     */
    private fun legacyDslProject(configure: Project.() -> Unit): ProjectInternal =
        buildProjectWithMPP(
            code = {
                kotlin { jvm() }
                configure()
            }
        ).also { it.evaluate() }
}

class LegacySwiftExportDslDetectionTests {

    @Test
    fun `test the legacy dsl is not reported as configured on a fresh project`() {
        val project = buildProjectWithMPP()
        assertFalse(project.legacySwiftExportExtension.isConfigured)
    }

    @Test
    fun `test setting the module name marks the legacy dsl as configured`() {
        val project = buildProjectWithMPP()
        project.legacySwiftExportExtension.moduleName.set("Legacy")

        assertTrue(project.legacySwiftExportExtension.isConfigured)
    }

    @Test
    fun `test setting the flatten package marks the legacy dsl as configured`() {
        val project = buildProjectWithMPP()
        project.legacySwiftExportExtension.flattenPackage.set("com.example.legacy")

        assertTrue(project.legacySwiftExportExtension.isConfigured)
    }

    @Test
    fun `test configuring advanced parameters marks the legacy dsl as configured`() {
        val project = buildProjectWithMPP()
        project.legacySwiftExportExtension.configure {
            freeCompilerArgs.add("-Xbinary=bundleId=com.example")
        }

        assertTrue(project.legacySwiftExportExtension.isConfigured)
    }

    @Test
    fun `test configuring the link task marks the legacy dsl as configured`() {
        val project = buildProjectWithMPP()
        project.legacySwiftExportExtension.linkTask { }

        assertTrue(project.legacySwiftExportExtension.isConfigured)
    }

    @Test
    fun `test exporting a dependency marks the legacy dsl as configured`() {
        val project = buildProjectWithMPP()
        project.legacySwiftExportExtension.export("org.example:lib:1.0")

        assertTrue(project.legacySwiftExportExtension.isConfigured)
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

    @Test
    fun `override renames a direct external api dependency`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            },
            swiftExport = {
                xcodeIntegration {
                    configure("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0") {
                        moduleName.set("ByteString")
                    }
                }
            }
        )

        project.evaluate()

        val actualModules = project.realizeSwiftModules()

        assertSetsEqual(
            setOf(
                ExportedSwiftModuleForAssertion(
                    moduleName = "ByteString",
                    artifactName = "kotlinx-io-bytestring-iosSimulatorArm64Main-0.7.0.klib",
                    shouldBeFullyExported = true,
                ),
            ),
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `override sets the root package of a direct external api dependency`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            },
            swiftExport = {
                xcodeIntegration {
                    configure("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0") {
                        rootPackage.set("kotlinx.io.bytestring")
                    }
                }
            }
        )

        project.evaluate()

        val actualModules = project.realizeSwiftModules()

        assertSetsEqual(
            setOf(
                ExportedSwiftModuleForAssertion(
                    moduleName = "OrgJetbrainsKotlinxKotlinxIoBytestring",
                    artifactName = "kotlinx-io-bytestring-iosSimulatorArm64Main-0.7.0.klib",
                    shouldBeFullyExported = true,
                    flattenPackage = "kotlinx.io.bytestring",
                ),
            ),
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `override applies regardless of the version it names`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            },
            swiftExport = {
                xcodeIntegration {
                    // Not the version in the graph: matching ignores it.
                    configure("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.1.0") {
                        moduleName.set("ByteString")
                    }
                }
            }
        )

        project.evaluate()

        val actualModules = project.realizeSwiftModules()

        assertSetsEqual(
            setOf(
                ExportedSwiftModuleForAssertion(
                    moduleName = "ByteString",
                    artifactName = "kotlinx-io-bytestring-iosSimulatorArm64Main-0.7.0.klib",
                    shouldBeFullyExported = true,
                ),
            ),
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `override through a dependency provider renames the module`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            }
        )
        // Exercises the Provider branch, the one a version catalog accessor
        // (Provider<MinimalExternalModuleDependency>) takes.
        project.exportExtension.swift {
            xcodeIntegration {
                configure(project.provider { project.dependencies.create("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0") }) {
                    moduleName.set("ByteString")
                }
            }
        }

        project.evaluate()

        val actualModules = project.realizeSwiftModules()

        assertSetsEqual(
            setOf(
                ExportedSwiftModuleForAssertion(
                    moduleName = "ByteString",
                    artifactName = "kotlinx-io-bytestring-iosSimulatorArm64Main-0.7.0.klib",
                    shouldBeFullyExported = true,
                ),
            ),
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `override renames a direct project api dependency`() {
        val project = buildProject(
            projectBuilder = { withName("shared") },
            configureProject = { configureRepositoriesForTests() }
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
            },
            swiftExport = {
                xcodeIntegration {
                    configure(projectDependency) {
                        moduleName.set("Renamed")
                        rootPackage.set("org.example.subproject")
                    }
                }
            }
        )

        project.evaluate()
        projectDependency.evaluate()

        val actualModules = project.realizeSwiftModules()

        assertSetsEqual(
            setOf(
                ExportedSwiftModuleForAssertion(
                    moduleName = "Renamed",
                    artifactName = "subproject",
                    shouldBeFullyExported = true,
                    flattenPackage = "org.example.subproject",
                ),
            ),
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `override renames a transitive dependency but its root package is ignored`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            },
            swiftExport = {
                xcodeIntegration {
                    configure("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0") {
                        moduleName.set("ByteString")
                        rootPackage.set("kotlinx.io.bytestring")
                    }
                }
            }
        )

        project.evaluate()

        val actualModules = project.realizeSwiftModules()

        assertSetsEqual(
            setOf(
                ExportedSwiftModuleForAssertion(
                    moduleName = "ByteString",
                    artifactName = "kotlinx-io-bytestring-iosSimulatorArm64Main-0.7.0.klib",
                    shouldBeFullyExported = false,
                    flattenPackage = null,
                ),
            ),
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `an override with an invalid module name is reported`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            },
            swiftExport = {
                xcodeIntegration {
                    configure("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0") {
                        moduleName.set("not-a-valid-swift-module")
                    }
                }
            }
        )

        project.evaluate()

        project.realizeSwiftModules()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftExportInvalidModuleName)
    }

    @Test
    fun `an override for a dependency absent from the graph is reported`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            },
            swiftExport = {
                xcodeIntegration {
                    configure("org.example:not-in-the-graph:1.0") { moduleName.set("Absent") }
                }
            }
        )

        project.evaluate()

        project.realizeSwiftModules()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftExportModuleResolutionError)
    }

    @Test
    fun `an override for a dependency that is never exported is reported`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                    // A JVM-only jar: it is in the graph but nothing is exported for it.
                    api("org.glassfish:jakarta.json:2.0.1")
                }
            },
            swiftExport = {
                xcodeIntegration {
                    configure("org.glassfish:jakarta.json:2.0.1") { moduleName.set("Json") }
                }
            }
        )

        project.evaluate()

        project.realizeSwiftModules()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftExportModuleResolutionError)
    }

    @Test
    fun `a matched override is not reported as unresolved`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            },
            swiftExport = {
                xcodeIntegration {
                    configure("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0") { moduleName.set("ByteString") }
                }
            }
        )

        project.evaluate()

        val actualModules = project.realizeSwiftModules()

        assertEquals(listOf("ByteString"), actualModules.map { it.moduleName })
        project.assertNoDiagnostics(KotlinToolingDiagnostics.SwiftExportModuleResolutionError)
    }

    @Test
    fun `an override written against an available-at target variant is applied`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            },
            swiftExport = {
                xcodeIntegration {
                    // The klib is published under the target-suffixed module and the root module redirects to it,
                    // so the override must apply through either name.
                    configure("org.jetbrains.kotlinx:kotlinx-io-bytestring-iossimulatorarm64:0.7.0") {
                        moduleName.set("ByteString")
                    }
                }
            }
        )

        project.evaluate()

        val actualModules = project.realizeSwiftModules()

        assertEquals(listOf("ByteString"), actualModules.map { it.moduleName })
        project.assertNoDiagnostics(KotlinToolingDiagnostics.SwiftExportModuleResolutionError)
    }

    @Test
    fun `two overrides producing the same module name fail`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                }
            },
            swiftExport = {
                xcodeIntegration {
                    configure("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0") { moduleName.set("Clash") }
                    configure("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0") { moduleName.set("Clash") }
                }
            }
        )

        project.evaluate()

        project.assertRealizingSwiftModulesFailsWith(KotlinToolingDiagnostics.SwiftExportDuplicateModuleNames)
    }

    @Test
    fun `an override clashing with the exported module name fails`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            },
            swiftExport = {
                moduleName.set("Shared")
                xcodeIntegration {
                    configure("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0") { moduleName.set("Shared") }
                }
            }
        )

        project.evaluate()

        project.assertRealizingSwiftModulesFailsWith(
            KotlinToolingDiagnostics.SwiftExportDuplicateModuleNames(
                mapOf("Shared" to listOf("the module being exported", "org.jetbrains.kotlinx:kotlinx-io-bytestring-iossimulatorarm64:0.7.0"))
            )
        )
    }

    @Test
    fun `module names differing only in case are duplicates`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            },
            swiftExport = {
                moduleName.set("Shared")
                xcodeIntegration {
                    configure("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0") { moduleName.set("shared") }
                }
            }
        )

        project.evaluate()

        project.assertRealizingSwiftModulesFailsWith(
            KotlinToolingDiagnostics.SwiftExportDuplicateModuleNames(
                mapOf("Shared/shared" to listOf("the module being exported", "org.jetbrains.kotlinx:kotlinx-io-bytestring-iossimulatorarm64:0.7.0"))
            )
        )
    }

    @Test
    fun `distinct module names are not reported as duplicates`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                }
            },
            swiftExport = {
                moduleName.set("Shared")
                xcodeIntegration {
                    configure("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0") { moduleName.set("ByteString") }
                }
            }
        )

        project.evaluate()

        val actualModules = project.realizeSwiftModules()

        assertEquals(listOf("ByteString"), actualModules.map { it.moduleName })
        project.assertNoDiagnostics(KotlinToolingDiagnostics.SwiftExportDuplicateModuleNames)
    }

    /** The export graph diagnostics are reported when the `swiftModules` provider is realized, not during configuration. */
    private fun Project.realizeSwiftModules(): List<SwiftExportedModule> =
        tasks.withType(SwiftExportTask::class.java).single().parameters.swiftModules.get()

    /**
     * Outside a real build the diagnostics collector turns a FATAL diagnostic into an exception right away, and
     * Gradle wraps it in a `PropertyQueryException`, hence the walk along the cause chain.
     */
    private fun Project.assertRealizingSwiftModulesFailsWith(diagnostic: ToolingDiagnosticFactory) =
        assertRealizingSwiftModulesFails { assertContainsDiagnostic(diagnostic) }

    private fun Project.assertRealizingSwiftModulesFailsWith(diagnostic: ToolingDiagnostic) =
        assertRealizingSwiftModulesFails { assertContainsDiagnostic(diagnostic) }

    private fun Project.assertRealizingSwiftModulesFails(assertDiagnostic: Project.() -> Unit) {
        val thrown = assertFails { realizeSwiftModules() }
        if (thrown.allCauses.none { it is InvalidUserCodeException }) {
            fail("Expected an InvalidUserCodeException in the cause chain, but got:\n${thrown.stackTraceToString()}")
        }
        assertDiagnostic()
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
        moduleName = module.moduleName,
        artifactName = module.artifact.name,
        shouldBeFullyExported = module.shouldBeFullyExported,
        flattenPackage = module.flattenPackage,
    )
}

private data class ExportedSwiftModuleForAssertion(
    val moduleName: String,
    val artifactName: String,
    val shouldBeFullyExported: Boolean,
    val flattenPackage: String? = null,
)
