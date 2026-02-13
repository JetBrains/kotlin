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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.ComputeLocalPackageDependencyInputFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftImportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.Test
import java.nio.file.Files
import org.jetbrains.kotlin.gradle.utils.normalizedAbsoluteFile
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SwiftPMImportUnitTests {

    @BeforeTest
    fun runOnMacOSOnly() {
        Assume.assumeTrue("macOS host required for this test", HostManager.hostIsMac)
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
                localPackage(
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
                localPackage(
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
                localPackage(
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
                localPackage(
                    directory = layout.projectDirectory.dir("nonExistentDirectory"),
                    products = listOf("SomeProduct"),
                )
            }
        )

        project.evaluate()

        // Verify diagnostic is emitted
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftPMLocalPackageDirectoryNotFound)
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
                localPackage(
                    directory = layout.projectDirectory.dir("packageWithoutManifest"),
                    products = listOf("SomeProduct"),
                )
            }
        )
        project.evaluate()

        // Verify diagnostic is emitted
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftPMLocalPackageMissingManifest)
    }

    @Test
    fun `test local package path pointing to file emits diagnostic`() {
        val project = swiftPMImportProject(
            preApplyCode = {
                val packageFile = project.projectDir.resolve("packageAsFile")
                packageFile.writeText("not a directory")
            },
            swiftPMDependencies = { layout ->
                localPackage(
                    directory = layout.projectDirectory.dir("packageAsFile"),
                    products = listOf("SomeProduct"),
                )
            }
        )
        project.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftPMLocalPackageMissingManifest)
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
                localPackage(
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
                localPackage(
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
                localPackage(
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
                localPackage(
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
                localPackage(
                    directory = layout.projectDirectory.dir("brokenLink"),
                    products = listOf("MissingPackage"),
                )
            }
        )

        project.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftPMLocalPackageDirectoryNotFound)
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
                localPackage(
                    directory = layout.projectDirectory.dir("blankNamePackage"),
                    products = listOf("BlankNamePackage"),
                    packageName = " ",
                )
            }
        )
        project.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftPMLocalPackageInvalidName)
    }
}

private fun ProjectInternal.swiftPmLocalDependencies(): List<SwiftPMDependency.Local> {
    val extension = kotlinExtension.getExtension<SwiftImportExtension>(SwiftImportExtension.EXTENSION_NAME)
    return requireNotNull(extension).spmDependencies.filterIsInstance<SwiftPMDependency.Local>()
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

    val fetchTask = tasks.withType(FetchSyntheticImportProjectPackages::class.java).single()
    val expectedManifests = expectedDirs.map { it.resolve("Package.swift").normalizedAbsoluteFile() }.toSet()
    val manifestFiles = fetchTask.localPackageManifests.files.map { it.normalizedAbsoluteFile() }.toSet()
    assertEquals(
        expected = expectedManifests,
        actual = manifestFiles,
        message = "${FetchSyntheticImportProjectPackages::class.java.simpleName}.${FetchSyntheticImportProjectPackages::localPackageManifests.name} should match configured package manifests"
    )
}

private fun swiftPMImportProject(
    projectBuilder: ProjectBuilder.() -> Unit = {},
    preApplyCode: Project.() -> Unit = {},
    multiplatform: KotlinMultiplatformExtension.() -> Unit = {
        iosSimulatorArm64()
    },
    swiftPMDependencies: SwiftImportExtension.(layout: ProjectLayout) -> Unit = {},
): ProjectInternal = buildProjectWithMPP(
    projectBuilder = projectBuilder,
    preApplyCode = {
        configureRepositoriesForTests()
        preApplyCode()
    },
    code = {
        kotlin {
            multiplatform()
            swiftImport {
                swiftPMDependencies(this@kotlin.project.layout)
            }
        }
    }
)
