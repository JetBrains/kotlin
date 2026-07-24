/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.ConvertSyntheticSwiftPMImportProjectIntoDefFile
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.ComputeLocalPackageDependencyInputFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.DumpXcodeBuildArgs
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FingerprintSyntheticPackage
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FingerprintXcodeBuild
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SerializeSwiftPMDependenciesMetadataForLockFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.ValidateLocalSwiftPMDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.transitiveSwiftPMMetadataProvider
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import kotlin.test.Test
import java.nio.file.Files
import org.jetbrains.kotlin.gradle.utils.normalizedAbsoluteFile
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class SwiftPMImportUnitTests {

    @BeforeTest
    fun runOnMacOSOnly() {
        Assumptions.assumeTrue(HostManager.hostIsMac, "macOS host required for this test")
    }

    @Test
    fun `test local package with relative path configures task correctly`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                // Create local Swift package directory with Package.swift (manifest name differs from directory)
                val localPackageDir = project.projectDir.resolve("localSwiftPackage")
                localPackageDir.mkdirs()
                localPackageDir.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 5.9
                    import PackageDescription
                    let package = Package(name: "LocalManifestPackage")
                    """.trimIndent()
                )
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("localSwiftPackage"),
                    products = listOf("LocalSwiftPackage"),
                )
            }
        )
        project.evaluate()

        // Verify task is registered
        val task = project.tasks.findByName(GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName)
        assertNotNull(task, "${GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName} task should be registered")
        assertIs<GenerateSyntheticLinkageImportProject>(task)

        val localPackageDir = project.projectDir.resolve("localSwiftPackage")
        project.assertLocalPackageTasksConfigured(setOf(localPackageDir))

        // Verify no error diagnostics
        project.assertNoDiagnostics()
    }

    @Test
    fun `test local package name inference from directory`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                // Create local Swift package directory with Package.swift (manifest name differs from directory)
                val localPackageDir = project.projectDir.resolve("my-custom-pkg")
                localPackageDir.mkdirs()
                localPackageDir.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 5.9
                    import PackageDescription
                    let package = Package(name: "ManifestPackage")
                    """.trimIndent()
                )
            },
            swiftPMDependencies = { layout ->
                // Don't specify packageName - should be inferred from directory name, not manifest name
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("my-custom-pkg"),
                    products = listOf("ManifestPackage"),
                )
            }
        )
        project.evaluate()

        // Verify no error diagnostics
        project.assertNoDiagnostics()

        val localDependency = project.swiftPmLocalDependencies().single()
        assertEquals("my-custom-pkg", localDependency.packageName)
        val localPackageDir = project.projectDir.resolve("my-custom-pkg")
        project.assertLocalPackageTasksConfigured(setOf(localPackageDir))
    }

    @Test
    fun `test local package with explicit package name`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                // Create local Swift package directory with Package.swift
                val localPackageDir = project.projectDir.resolve("some-directory")
                localPackageDir.mkdirs()
                localPackageDir.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 5.9
                    import PackageDescription
                    let package = Package(name: "ActualPackageName")
                    """.trimIndent()
                )
            },
            swiftPMDependencies = { layout ->
                // Specify explicit packageName different from directory name
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("some-directory"),
                    products = listOf("ActualPackageName"),
                    packageName = "ExplicitCustomName",
                )
            }
        )
        project.evaluate()

        // Verify no error diagnostics - explicit name is valid
        project.assertNoDiagnostics()

        val localDependency = project.swiftPmLocalDependencies().single()
        assertEquals("ExplicitCustomName", localDependency.packageName)
        val localPackageDir = project.projectDir.resolve("some-directory")
        project.assertLocalPackageTasksConfigured(setOf(localPackageDir))
    }

    @Test
    fun `test local package directory not found emits diagnostic`() {
        val project = swiftPMImportProject(
            swiftPMDependencies = { layout ->
                // Reference a directory that doesn't exist
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("nonExistentDirectory"),
                    products = listOf("SomeProduct"),
                )
            }
        )

        project.evaluate()
        // No diagnostics at configuration time — validation runs when the task executes
        project.assertNoDiagnostics()

        val validateTask = project.tasks.findByName(ValidateLocalSwiftPMDependencies.TASK_NAME)
        assertNotNull(validateTask, "${ValidateLocalSwiftPMDependencies.TASK_NAME} should be registered")
        assertIs<ValidateLocalSwiftPMDependencies>(validateTask)

        val computeTask = project.tasks.withType(ComputeLocalPackageDependencyInputFiles::class.java).single()
        val nonExistentDir = project.projectDir.resolve("nonExistentDirectory").normalizedAbsoluteFile()
        assertTrue(
            nonExistentDir in computeTask.localPackages.get().map { it.normalizedAbsoluteFile() }.toSet(),
            "Non-existent local package dir should still be wired into computeLocalPackageDependencyInputFiles"
        )

        val exception = assertFailsWith<Exception> {
            validateTask.validate()
        }
        assertContains(exception.message.orEmpty(), KotlinToolingDiagnostics.SwiftPMLocalPackageDirectoryNotFound.id)
    }

    @Test
    fun `test local package missing manifest emits diagnostic`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                // Create directory but don't create Package.swift
                val localPackageDir = project.projectDir.resolve("packageWithoutManifest")
                localPackageDir.mkdirs()
                // Intentionally NOT creating Package.swift
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("packageWithoutManifest"),
                    products = listOf("SomeProduct"),
                )
            }
        )
        project.evaluate()

        project.assertNoDiagnostics()

        val validateTask = project.tasks.findByName(ValidateLocalSwiftPMDependencies.TASK_NAME)
        assertNotNull(validateTask)
        assertIs<ValidateLocalSwiftPMDependencies>(validateTask)

        val exception = assertFailsWith<Exception> {
            validateTask.validate()
        }
        assertContains(exception.message.orEmpty(), KotlinToolingDiagnostics.SwiftPMLocalPackageMissingManifest.id)
    }

    @Test
    fun `test local package path pointing to file emits diagnostic`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                val packageFile = project.projectDir.resolve("packageAsFile")
                packageFile.writeText("not a directory")
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("packageAsFile"),
                    products = listOf("SomeProduct"),
                )
            }
        )
        project.evaluate()

        project.assertNoDiagnostics()

        val validateTask = project.tasks.findByName(ValidateLocalSwiftPMDependencies.TASK_NAME)
        assertNotNull(validateTask)
        assertIs<ValidateLocalSwiftPMDependencies>(validateTask)

        val exception = assertFailsWith<Exception> {
            validateTask.validate()
        }
        assertContains(exception.message.orEmpty(), KotlinToolingDiagnostics.SwiftPMLocalPackageMissingManifest.id)
    }

    @Test
    fun `test local package directory with spaces resolves correctly`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                // Manifest name differs from directory name to avoid inference ambiguity
                val localPackageDir = project.projectDir.resolve("Package With Spaces")
                localPackageDir.mkdirs()
                localPackageDir.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 5.9
                    import PackageDescription
                    let package = Package(name: "SpacesManifest")
                    """.trimIndent()
                )
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("Package With Spaces"),
                    products = listOf("PackageWithSpaces"),
                )
            }
        )
        project.evaluate()

        project.assertNoDiagnostics()
        val localPackageDir = project.projectDir.resolve("Package With Spaces")
        project.assertLocalPackageTasksConfigured(setOf(localPackageDir))
    }

    @Test
    fun `test local package with sibling directory path`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                // Create local Swift package as a sibling directory (using ../)
                // Manifest name differs from directory name to avoid inference ambiguity
                val siblingPackageDir = project.projectDir.parentFile.resolve("siblingSwiftPackage")
                siblingPackageDir.mkdirs()
                siblingPackageDir.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 5.9
                    import PackageDescription
                    let package = Package(name: "SiblingManifest")
                    """.trimIndent()
                )
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("../siblingSwiftPackage"),
                    products = listOf("SiblingSwiftPackage"),
                )
            }
        )
        project.evaluate()

        // Verify no error diagnostics
        project.assertNoDiagnostics()

        // Verify task is registered
        val task = project.tasks.findByName(GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName)
        assertNotNull(task, "Task should be registered when local package is configured with relative path")

        val siblingPackageDir = project.projectDir.parentFile.resolve("siblingSwiftPackage")
        project.assertLocalPackageTasksConfigured(setOf(siblingPackageDir))
    }

    @Test
    fun `test local package name inference with dot-dot path resolves correctly`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                // Create Package.swift in parent directory
                // The parent directory name will be used as the package name
                project.projectDir.parentFile.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 5.9
                    import PackageDescription
                    let package = Package(name: "${project.projectDir.parentFile.name}")
                    """.trimIndent()
                )
            },
            swiftPMDependencies = { layout ->
                // Use ".." which should resolve to parent directory name, NOT ".."
                localSwiftPackage(
                    directory = layout.projectDirectory.dir(".."),
                    products = listOf("ParentProduct"),
                )
            }
        )
        project.evaluate()

        // Verify no error diagnostics - specifically no invalid name diagnostic
        // Without toAbsolutePath().normalize().fileName, the inferred name would be ".." which is invalid
        project.assertNoDiagnostics()

        val localDependency = project.swiftPmLocalDependencies().single()
        assertEquals(project.projectDir.parentFile.name, localDependency.packageName)

        project.assertLocalPackageTasksConfigured(setOf(project.projectDir.parentFile))

        // Verify task is registered
        val task = project.tasks.findByName(GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName)
        assertNotNull(task, "Task should be registered when local package uses '..' path")
    }

    @Test
    fun `test local package via symlink preserves unresolved path`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                val projectDir = project.projectDir
                val realPackageDir = projectDir.resolve("realPackage")
                val symlinkDir = projectDir.resolve("symlinkPackage")
                realPackageDir.mkdirs()
                realPackageDir.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 5.9
                    import PackageDescription
                    let package = Package(name: "RealPackage")
                    """.trimIndent()
                )
                Files.createSymbolicLink(symlinkDir.toPath(), realPackageDir.toPath())
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("symlinkPackage"),
                    products = listOf("RealPackage"),
                )
            }
        )

        project.evaluate()

        project.assertNoDiagnostics()
        // Configuration-time path storage should preserve the symlink as-is.
        val symlinkDir = project.projectDir.resolve("symlinkPackage")
        project.assertLocalPackageTasksConfigured(setOf(symlinkDir))
    }

    @Test
    fun `test local package broken symlink emits diagnostic`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                val projectDir = project.projectDir
                val brokenSymlinkDir = projectDir.resolve("brokenLink")
                val missingTargetDir = projectDir.resolve("missingTarget")
                Files.createSymbolicLink(brokenSymlinkDir.toPath(), missingTargetDir.toPath())
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("brokenLink"),
                    products = listOf("MissingPackage"),
                )
            }
        )

        project.evaluate()

        project.assertNoDiagnostics()

        val validateTask = project.tasks.findByName(ValidateLocalSwiftPMDependencies.TASK_NAME)
        assertNotNull(validateTask)
        assertIs<ValidateLocalSwiftPMDependencies>(validateTask)

        val exception = assertFailsWith<Exception> {
            validateTask.validate()
        }
        assertContains(exception.message.orEmpty(), KotlinToolingDiagnostics.SwiftPMLocalPackageDirectoryNotFound.id)
    }

    @Test
    fun `test local package with blank package name emits diagnostic`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                val localPackageDir = project.projectDir.resolve("blankNamePackage")
                localPackageDir.mkdirs()
                localPackageDir.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 5.9
                    import PackageDescription
                    let package = Package(name: "BlankNamePackage")
                    """.trimIndent()
                )
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("blankNamePackage"),
                    products = listOf("BlankNamePackage"),
                    packageName = " ",
                )
            }
        )
        project.evaluate()

        project.assertNoDiagnostics()

        val validateTask = project.tasks.findByName(ValidateLocalSwiftPMDependencies.TASK_NAME)
        assertNotNull(validateTask)
        assertIs<ValidateLocalSwiftPMDependencies>(validateTask)

        val exception = assertFailsWith<Exception> {
            validateTask.validate()
        }
        assertContains(exception.message.orEmpty(), KotlinToolingDiagnostics.SwiftPMLocalPackageInvalidName.id)
    }

    @Test
    fun `ios cinterop depends on ios def task but not macos def task`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                val localPackageDir = project.projectDir.resolve("packageOne")
                localPackageDir.mkdirs()
                localPackageDir.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 5.9
                    import PackageDescription
                    let package = Package(name: "packageOne")
                    """.trimIndent()
                )
            },
            multiplatform = {
                iosArm64()
                macosArm64()
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("packageOne"),
                    products = listOf(
                        SwiftPMDependency.Product(
                            name = "packageOne",
                            platformConstraints = setOf(SwiftPMDependency.Platform.iOS),
                        )
                    ),
                )
            }
        )
        project.evaluate()

        val iosDefTask = project.assertContainsTaskInstance<ConvertSyntheticSwiftPMImportProjectIntoDefFile>(
            "convertSyntheticImportProjectIntoDefFileIphoneos"
        )
        val macosDefTask = project.assertContainsTaskInstance<ConvertSyntheticSwiftPMImportProjectIntoDefFile>(
            "convertSyntheticImportProjectIntoDefFileMacosx"
        )
        val iosCinteropTask = project.assertContainsTaskWithName("cinteropSwiftPMImportIosArm64")
        val macosCinteropTask = project.assertContainsTaskWithName("cinteropSwiftPMImportMacosArm64")

        iosCinteropTask.assertDependsOn(iosDefTask)
        iosCinteropTask.assertNotDependsOn(macosDefTask)
        macosCinteropTask.assertDependsOn(macosDefTask)
        macosCinteropTask.assertNotDependsOn(iosDefTask)
    }

    @Test
    fun `test - only dynamic frameworks depend on SwiftPM import pipeline`() {
        val linkTaskName = "linkDebugFrameworkIosSimulatorArm64"
        val staticFramework = swiftPMImportProject(
            multiplatform = {
                iosSimulatorArm64().binaries.framework {
                    isStatic = true
                }
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("my-custom-pkg"),
                    products = listOf("ManifestPackage"),
                )
            }
        ).evaluate()

        val staticFrameworkTaskDependencies = staticFramework.tasks.getByName(linkTaskName)
            .taskDependencies.getDependencies(null)
            .map { it.name }.toSet()

        val dynamicFramework = swiftPMImportProject(
            multiplatform = {
                iosSimulatorArm64().binaries.framework {
                    isStatic = false
                }
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("my-custom-pkg"),
                    products = listOf("ManifestPackage"),
                )
            }
        ).evaluate()

        val dynamicFrameworkTaskDependencies = dynamicFramework.tasks.getByName(linkTaskName)
            .taskDependencies.getDependencies(null)
            .map { it.name }.toSet()

        assertEquals(
            setOf("convertSyntheticImportProjectIntoDefFileIphonesimulator"),
            dynamicFrameworkTaskDependencies - staticFrameworkTaskDependencies,
        )
        assertEquals(
            setOf(),
            staticFrameworkTaskDependencies - dynamicFrameworkTaskDependencies,
        )
    }

    @Test
    fun `swiftPM interproject metadata task dependencies - lock serialization depends on regular metadata and root depends on serialization for locks` () {
        val rootProject = buildProjectWithMPP {
            kotlin {
                iosSimulatorArm64()

                swiftPMDependencies {
                    swiftPackage("foo", "1.0.0", listOf())
                }
            }
        }.evaluate()

        buildProjectWithMPP(
            projectBuilder = {
                withParent(rootProject)
                withName("subprojectTransitive")
            }
        ) {
            kotlin {
                iosSimulatorArm64()

                swiftPMDependencies {
                    swiftPackage("foo", "1.0.0", listOf())
                }
            }
        }.evaluate()

        val subprojectDirect = buildProjectWithMPP(
            projectBuilder = {
                withName("subprojectDirect")
                withParent(rootProject)
            }
        ) {
            kotlin {
                iosSimulatorArm64()
                swiftPMDependencies {
                    swiftPackage("foo", "1.0.0", listOf())
                }

                sourceSets.commonMain.dependencies {
                    implementation(project(":subprojectTransitive"))
                }
            }
        }.evaluate()

        buildProjectWithMPP(
            projectBuilder = {
                withName("subprojectUnrelated")
                withParent(rootProject)
            }
        ) {
            kotlin {
                iosSimulatorArm64()
            }
        }.evaluate()

        // Subproject's direct serialization for umbrella should a dependency on regular serialize metadata task because it has a dependency on subproject transitive
        assertEquals(
            setOf(":subprojectTransitive:serializeSwiftPMDependenciesMetadata"),
            subprojectDirect.tasks.withType(SerializeSwiftPMDependenciesMetadataForLockFiles::class.java)
                .single().taskDependencies.getDependencies(null).map { it.path }.toSet(),
        )

        // Then umbrella task should be in the root and should depend on everyone
        assertEquals(
            setOf(
                ":serializeSwiftPMDependenciesMetadataForLockFiles",
                ":subprojectTransitive:serializeSwiftPMDependenciesMetadataForLockFiles",
                ":subprojectDirect:serializeSwiftPMDependenciesMetadataForLockFiles",
                ":subprojectUnrelated:serializeSwiftPMDependenciesMetadataForLockFiles",
            ),
            rootProject.tasks.withType(GenerateSyntheticLinkageImportProject::class.java)
                .single { "Umbrella" in it.name }.taskDependencies.getDependencies(null).map { it.path }.toSet(),
        )
    }

    @Test
    fun `KT-85517 - swiftPM metadata resolution doesn't fail on accidentally resolve outgoing variants without Usage`() {
        val rootProject = buildProject {
            plugins.apply("java-library")
            project.configurations.create("consumable") {
                it.outgoing.artifact(file("foo"))
                it.attributes.attribute(org.gradle.api.attributes.Attribute.of("foo", String::class.java), "bar")
            }
        }.evaluate()

        val swiftPMConsumer = swiftPMImportProject(
            projectBuilder = {
                withParent(rootProject)
            },
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation(project(":"))
                }
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("my-custom-pkg"),
                    products = listOf("ManifestPackage"),
                )
            }
        ).evaluate()

        assertDoesNotThrow {
            swiftPMConsumer.transitiveSwiftPMMetadataProvider().get()
        }
    }

    @Test
    fun `KT-85561 - umbrella task is only registered for Apple SwiftPM projects and not for non-Apple consumers`() {
        val identifier = "default"

        val rootProject = buildProject {
            configureRepositoriesForTests()
        }

        val shared = swiftPMImportProject(
            projectBuilder = {
                withParent(rootProject)
                withName("shared")
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("my-custom-pkg"),
                    products = listOf("ManifestPackage"),
                )
            }
        )

        val composeApp = buildProjectWithMPP(
            projectBuilder = {
                withParent(rootProject)
                withName("composeApp")
            },
            preApplyCode = {
                configureRepositoriesForTests()
            },
            code = {
                kotlin {
                    jvm()

                    sourceSets.getByName("commonMain").dependencies {
                        implementation(project(":shared"))
                    }
                }
            }
        )

        shared.evaluate()
        composeApp.evaluate()

        val umbrellaTask = shared.assertContainsTaskInstance<GenerateSyntheticLinkageImportProject>(
            GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(identifier)
        )

        assertDoesNotThrow {
            umbrellaTask.taskDependencies.getDependencies(umbrellaTask)
        }
    }

    @Test
    fun `test umbrella fetch task is registered with correct gitignore configuration`() {
        val identifier = "default"
        val project = swiftPMImportProject(
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("my-custom-pkg"),
                    products = listOf("ManifestPackage"),
                )
            }
        ).evaluate()


        val umbrellaFetchTask = project.tasks.findByName(
            FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(identifier)
        )

        val syntheticFetchTask = project.tasks.findByName(
            FetchSyntheticImportProjectPackages.TASK_NAME
        )

        val convertDefTask = project.tasks.findByName(
            lowerCamelCaseName(
                ConvertSyntheticSwiftPMImportProjectIntoDefFile.TASK_NAME,
                "iphonesimulator"
            )
        )

        assertNotNull(umbrellaFetchTask, "Umbrella fetch task should be registered")
        assertIs<FetchSyntheticImportProjectPackages>(umbrellaFetchTask)

        assertNotNull(syntheticFetchTask, "Synthetic fetch task should be registered")
        assertIs<FetchSyntheticImportProjectPackages>(syntheticFetchTask)

        assertNotNull(convertDefTask, "Convert task should be registered")
        assertIs<ConvertSyntheticSwiftPMImportProjectIntoDefFile>(convertDefTask)

        assertTrue(
            umbrellaFetchTask.gitIgnoreCheckoutDir.get(),
            "Umbrella fetch task should enable gitIgnoreCheckoutDir"
        )
    }


    @Test
    fun `test umbrella fetch task is registered separately per identifier with isolated checkout dirs`() {
        val fuzzIdentifier = "fuzzLock"
        val buzzIdentifier = "buzzLock"
        val rootProject = buildProject { configureRepositoriesForTests() }

        val fuzzProject = swiftPMImportProject(
            projectBuilder = { withParent(rootProject); withName("fuzz") },
            swiftPMDependencies = { layout ->
                packageResolvedSynchronization = identifier(fuzzIdentifier)
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("my-pkg"),
                    products = listOf("FuzzPackage")
                )
            }
        ).evaluate()

        val buzzProject = swiftPMImportProject(
            projectBuilder = { withParent(rootProject); withName("buzz") },
            swiftPMDependencies = { layout ->
                packageResolvedSynchronization = identifier(buzzIdentifier)
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("buzz-pkg"),
                    products = listOf("BuzzPackage")
                )
            }
        ).evaluate()

        val umbrellaFuzzFetchTask = fuzzProject.tasks.findByName(
            FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(fuzzIdentifier)
        )
        val umbrellaBuzzFetchTask = buzzProject.tasks.findByName(
            FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(buzzIdentifier)
        )

        val syntheticFuzzFetchTask = fuzzProject.tasks.findByName(
            FetchSyntheticImportProjectPackages.TASK_NAME
        )
        val syntheticBuzzFetchTask = buzzProject.tasks.findByName(
            FetchSyntheticImportProjectPackages.TASK_NAME
        )

        val fuzzConvertDefTask = fuzzProject.tasks.findByName(
            lowerCamelCaseName(
                ConvertSyntheticSwiftPMImportProjectIntoDefFile.TASK_NAME,
                "iphonesimulator"
            )
        )

        val buzzConvertDefTask = buzzProject.tasks.findByName(
            lowerCamelCaseName(
                ConvertSyntheticSwiftPMImportProjectIntoDefFile.TASK_NAME,
                "iphonesimulator"
            )
        )

        assertNotNull(umbrellaFuzzFetchTask)
        assertNotNull(umbrellaBuzzFetchTask)
        assertIs<FetchSyntheticImportProjectPackages>(umbrellaFuzzFetchTask)
        assertIs<FetchSyntheticImportProjectPackages>(umbrellaBuzzFetchTask)
        assertNotSame(umbrellaFuzzFetchTask, umbrellaBuzzFetchTask, "Each identifier must have its own umbrella fetch task instance")

        assertNotNull(syntheticFuzzFetchTask)
        assertNotNull(syntheticBuzzFetchTask)
        assertIs<FetchSyntheticImportProjectPackages>(syntheticFuzzFetchTask)
        assertIs<FetchSyntheticImportProjectPackages>(syntheticBuzzFetchTask)


        assertNotNull(fuzzConvertDefTask, "Fuzz convert task should be registered")
        assertIs<ConvertSyntheticSwiftPMImportProjectIntoDefFile>(fuzzConvertDefTask)

        assertNotNull(buzzConvertDefTask, "Buzz convert task should be registered")
        assertIs<ConvertSyntheticSwiftPMImportProjectIntoDefFile>(buzzConvertDefTask)

        assertTrue(umbrellaFuzzFetchTask.gitIgnoreCheckoutDir.get(), "Fuzz fetch task should enable gitIgnoreCheckoutDir")
        assertTrue(umbrellaBuzzFetchTask.gitIgnoreCheckoutDir.get(), "Buzz fetch task should enable gitIgnoreCheckoutDir")
    }

    @Test
    fun `fetch task call triggers synthetic package fingerprint task`() {

        val project = swiftPMImportProject(
            multiplatform = {
                iosArm64()
                iosSimulatorArm64()
            },
            preApplyCode = {
                val localPackageDir = project.projectDir.resolve("mapsPackage")
                localPackageDir.mkdirs()
                localPackageDir.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 5.9
                    import PackageDescription
                    let package = Package(name: "MapsPackage")
                    """.trimIndent()
                )
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("mapsPackage"),
                    products = listOf("MapsPackage"),
                    packageName = "MapsPackage",
                )
            }

        ).evaluate()


        val iphoneSimulatorFingerprintTask = project.tasks.findByName(
            FingerprintSyntheticPackage.TASK_NAME
        )

        val iphoneSimulatorDumpTask = project.tasks.findByName(
            lowerCamelCaseName(DumpXcodeBuildArgs.TASK_NAME, "iphonesimulator")
        )

        val iphoneosDumpTask = project.tasks.findByName(
            lowerCamelCaseName(DumpXcodeBuildArgs.TASK_NAME, "iphoneos")
        )

        val fetchTask = project.tasks.findByName(
            "fetchSyntheticImportProjectPackages"
        )

        assertIs<FetchSyntheticImportProjectPackages>(fetchTask)
        assertIs<FingerprintSyntheticPackage>(iphoneSimulatorFingerprintTask)

        assertIs<DumpXcodeBuildArgs>(iphoneosDumpTask)
        assertIs<DumpXcodeBuildArgs>(iphoneSimulatorDumpTask)

        fetchTask.assertDependsOn(
            iphoneSimulatorFingerprintTask,
        )

        assertEquals(
            iphoneSimulatorFingerprintTask.syntheticPackageFingerprint.get().asFile,
            iphoneSimulatorDumpTask.syntheticPackageFingerprint.get().asFile,
            "Fingerprint hash and dump task for iphonesimulator should match"
        )
    }

    @Test
    fun `project with noSync package resolved synchronization should not register coordination tasks`() {
        val project = swiftPMImportProject(
            swiftPMDependencies = { layout ->
                packageResolvedSynchronization = noSynchronization()

                localSwiftPackage(
                    directory = layout.projectDirectory.dir("my-custom-pkg"),
                    products = listOf("ManifestPackage"),
                )
            }
        ).evaluate()

        val syntheticPackageGenerationTask = project.tasks.findByName(
            GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName
        )

        assertIs<GenerateSyntheticLinkageImportProject>(syntheticPackageGenerationTask)

        assertFalse(
            syntheticPackageGenerationTask.syntheticPackageFingerprint.isPresent,
            message = "Synthetic package fingerprint should not be set for synthetic package generation when noSynchronization is set"
        )

        val fetchSyntheticPackageTask = project.tasks.findByName(
            FetchSyntheticImportProjectPackages.TASK_NAME
        )

        assertIs<FetchSyntheticImportProjectPackages>(fetchSyntheticPackageTask)

        assertFalse(
            fetchSyntheticPackageTask.syntheticPackageFingerprint.isPresent,
            message = "Synthetic package fingerprint should not be set for fetch task when noSynchronization is set"
        )

        val dumpXcodebuildTask = project.tasks.findByName(
            lowerCamelCaseName(
                DumpXcodeBuildArgs.TASK_NAME,
                "iphonesimulator"
            )
        )

        assertIs<DumpXcodeBuildArgs>(dumpXcodebuildTask)

        assertFalse(
            dumpXcodebuildTask.syntheticPackageFingerprint.isPresent,
            message = "Synthetic package fingerprint should not be set for dump task when noSynchronization is set"
        )

        assertFalse(
            dumpXcodebuildTask.xcodebuildFingerprint.isPresent,
            message = "Xcodebuild fingerprint should not be set for dump task when noSynchronization is set"
        )
    }

    @Test
    fun `projects use correct synthetic package fingerprint and xcodebuild fingerprint`() {

        val rootProject = buildProject { configureRepositoriesForTests() }

        swiftPMImportProject(
            projectBuilder = {
                withParent(rootProject)
                withName("kmp-maps")
            },
            preApplyCode = {
                val localPackageDir = project.projectDir.resolve("mapsPackage")
                localPackageDir.mkdirs()
                localPackageDir.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 5.9
                    import PackageDescription
                    let package = Package(name: "MapsPackage")
                    """.trimIndent()
                )
            },
            swiftPMDependencies = { layout ->
                localSwiftPackage(
                    directory = layout.projectDirectory.dir("mapsPackage"),
                    products = listOf("MapsPackage"),
                    packageName = "MapsPackage",
                )
            }
        )

        val leftProject = swiftPMImportProject(
            projectBuilder = {
                withParent(rootProject)
                withName("left")
            },
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.getByName("iosSimulatorArm64Main").dependencies {
                    implementation(project(":kmp-maps"))
                }
            }
        )

        leftProject.evaluate()

        val leftProjectSyntheticPackageFingerprint = leftProject.tasks.findByName(
            FingerprintSyntheticPackage.TASK_NAME
        )

        assertIs<FingerprintSyntheticPackage>(leftProjectSyntheticPackageFingerprint)

        val leftProjectSyntheticPackageGenerate = leftProject.tasks.findByName(
            GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName
        )

        assertIs<GenerateSyntheticLinkageImportProject>(leftProjectSyntheticPackageGenerate)

        val leftProjectFetchTask = leftProject.tasks.findByName(
            FetchSyntheticImportProjectPackages.TASK_NAME
        )

        assertIs<FetchSyntheticImportProjectPackages>(leftProjectFetchTask)

        val leftProjectXcodebuildFingerprint = leftProject.tasks.findByName(
            lowerCamelCaseName(FingerprintXcodeBuild.TASK_NAME, "iphonesimulator")
        )

        assertIs<FingerprintXcodeBuild>(leftProjectXcodebuildFingerprint)

        val leftProjectXcodeDumpTask = leftProject.tasks.findByName(
            lowerCamelCaseName(
                DumpXcodeBuildArgs.TASK_NAME,
                "iphonesimulator",
            )
        )

        assertIs<DumpXcodeBuildArgs>(leftProjectXcodeDumpTask)

        val leftProjectConvertTask = leftProject.tasks.findByName(
            lowerCamelCaseName(
                ConvertSyntheticSwiftPMImportProjectIntoDefFile.TASK_NAME,
                "iphonesimulator",
            )
        )

        assertIs<ConvertSyntheticSwiftPMImportProjectIntoDefFile>(leftProjectConvertTask)

        assertEquals(
            leftProjectSyntheticPackageFingerprint.syntheticPackageFingerprint.get().asFile,
            leftProjectSyntheticPackageGenerate.syntheticPackageFingerprint.get().asFile,
            "Generate synthetic package tasks should use the same synthetic package fingerprint as the fingerprint task in the same project"
        )

        assertEquals(
            leftProjectSyntheticPackageGenerate.syntheticPackageFingerprint.get().asFile,
            leftProjectFetchTask.syntheticPackageFingerprint.get().asFile,
            "Fetch task should use the same synthetic package fingerprint as the generate task in the same project"
        )

        assertEquals(
            leftProjectXcodebuildFingerprint.syntheticPackageFingerprint.get().asFile,
            leftProjectFetchTask.syntheticPackageFingerprint.get().asFile,
            "Xcodebuild fingerprint task should use the same synthetic package fingerprint as the fetch task in the same project"
        )

        assertEquals(
            leftProjectXcodeDumpTask.xcodebuildFingerprint.get().asFile,
            leftProjectXcodebuildFingerprint.xcodebuildFingerprint.get().asFile,
            "Dump task should use the same xcodebuild fingerprint as the fingerprint task in the same project"
        )

        assertEquals(
            leftProjectConvertTask.xcodebuildFingerprint.get().asFile,
            leftProjectXcodeDumpTask.xcodebuildFingerprint.get().asFile,
            "Convert task should use the same xcodebuild fingerprint as the dump task in the same project"
        )


    }
}

private fun ProjectInternal.swiftPmLocalDependencies(): List<SwiftPMDependency.Local> {
    val extension = kotlinExtension.getExtension<SwiftPMImportExtension>(SwiftPMImportExtension.EXTENSION_NAME)
    return requireNotNull(extension).swiftPMDependencies.filterIsInstance<SwiftPMDependency.Local>()
}

private fun ProjectInternal.assertLocalPackageTasksConfigured(expectedPackageDirs: Set<java.io.File>) {
    val expectedDirs = expectedPackageDirs.map { it.normalizedAbsoluteFile() }.toSet()
    val computeTask = tasks.withType(ComputeLocalPackageDependencyInputFiles::class.java).single()
    val localPackages = computeTask.localPackages.get().map { it.normalizedAbsoluteFile() }.toSet()
    assertEquals(
        expected = expectedDirs,
        actual = localPackages,
        message = "${ComputeLocalPackageDependencyInputFiles::class.java.simpleName}.${ComputeLocalPackageDependencyInputFiles::localPackages.name} should match configured local packages"
    )

    val fetchTask = tasks.withType(FetchSyntheticImportProjectPackages::class.java).single {
        it.name.equals(FetchSyntheticImportProjectPackages.TASK_NAME, ignoreCase = true)
    }

    val expectedManifests = expectedDirs.map { it.resolve("Package.swift").normalizedAbsoluteFile() }.toSet()
    val manifestFiles = fetchTask.localPackageManifests.files.map { it.normalizedAbsoluteFile() }.toSet()
    assertEquals(
        expected = expectedManifests,
        actual = manifestFiles,
        message = "${FetchSyntheticImportProjectPackages::class.java.simpleName}.${FetchSyntheticImportProjectPackages::localPackageManifests.name} should match configured package manifests"
    )

    val validateTask = tasks.findByName(ValidateLocalSwiftPMDependencies.TASK_NAME)
    assertNotNull(validateTask, "${ValidateLocalSwiftPMDependencies.TASK_NAME} task should be registered")
    assertIs<ValidateLocalSwiftPMDependencies>(validateTask)
    computeTask.assertDependsOn(validateTask)
}

private fun swiftPMImportProject(
    projectBuilder: ProjectBuilder.() -> Unit = {},
    preApplyCode: Project.() -> Unit = {},
    multiplatform: KotlinMultiplatformExtension.() -> Unit = {
        iosSimulatorArm64()
    },
    swiftPMDependencies: SwiftPMImportExtension.(layout: ProjectLayout) -> Unit = {},
): ProjectInternal = buildProjectWithMPP(
    projectBuilder = projectBuilder,
    preApplyCode = {
        configureRepositoriesForTests()
        preApplyCode()
    },
    code = {
        kotlin {
            multiplatform()
            swiftPMDependencies {
                swiftPMDependencies(this@kotlin.project.layout)
            }
        }
    }
)
