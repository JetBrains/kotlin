/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalSerializationApi::class, ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.register
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.createKotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.Companion.SyntheticProductType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependencyIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.TransitiveSwiftPMMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.String
import kotlin.io.path.createDirectories
import kotlin.io.path.isWritable
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class GenerateSyntheticLinkageImportProjectTests : KGPBaseTest() {

    @GradleTest
    fun `package generation with direct dependencies`(version: GradleVersion) {
        project("empty", version) {
            val packageOne = projectPath.resolve("packageOne").also { it.createDirectories() }.toFile()
            runProcess(listOf("swift", "package", "init", "--type", "library"), packageOne)

            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.createKotlinExtension(KotlinMultiplatformExtension::class)
                val extension = project.locateOrRegisterSwiftPMDependenciesExtension().apply {
                    iosMinimumDeploymentTarget.set("123.0")
                    tvosMinimumDeploymentTarget.set("234.0")
                    swiftPackage("https://foo.bar/baz.git", "1.0.0", listOf("a", "b"))
                    localSwiftPackage(project.layout.projectDirectory.dir("packageOne"), listOf("packageOne"))
                }
                project.tasks.register<GenerateSyntheticLinkageImportProject>("packageGeneration") {
                    configureWithExtension(extension)
                    konanTargets.set(setOf(KonanTarget.IOS_ARM64))
                    transitiveSwiftPMMetadata.set(TransitiveSwiftPMMetadata(emptyMap()))
                    syntheticProductType.set(SyntheticProductType.INFERRED)
                }
            }
            build("packageGeneration")

            val generatedPackage = json.decodeFromString<PackageDescription>(
                runProcess(
                    listOf("swift", "package", "describe", "--type", "json"),
                    projectPath.resolve("build/kotlin/swiftImport").toFile(),
                ).output
            )

            assertEquals(
                PackageDescription(
                    dependencies = listOf(
                        PackageDescription.PackageDependency(
                            identity = "baz",
                            path = null,
                            type = "sourceControl",
                            url = "https://foo.bar/baz.git",
                        ),
                        PackageDescription.PackageDependency(
                            identity = "packageone",
                            path = packageOne.toPath().toRealPath().pathString,
                            type = "fileSystem",
                            url = null,
                        ),
                    ),
                    platforms = listOf(
                        PackageDescription.PackagePlatform(
                            name = "ios",
                            version = "123.0",
                        ),
                    ),
                    products = listOf(
                        PackageDescription.PackageProduct(
                            name = "KotlinMultiplatformLinkedPackage",
                            targets = listOf(
                                "KotlinMultiplatformLinkedPackage",
                            ),
                            type = mapOf(
                                "library" to listOf(
                                    "automatic",
                                ),
                            ),
                        ),
                    ),
                    targets = listOf(
                        PackageDescription.PackageTarget(
                            path = "Sources/KotlinMultiplatformLinkedPackage",
                            productDependencies = listOf(
                                "a",
                                "b",
                                "packageOne",
                            ),
                            type = "library",
                        ),
                    ),
                ).prettyPrinted,
                generatedPackage.prettyPrinted
            )
        }
    }

    @GradleTest
    fun `package generation with transitive dependencies`(version: GradleVersion) {
        project("empty", version) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.createKotlinExtension(KotlinMultiplatformExtension::class)
                val extension = project.locateOrRegisterSwiftPMDependenciesExtension()
                project.tasks.register<GenerateSyntheticLinkageImportProject>("packageGeneration") {
                    configureWithExtension(extension)
                    konanTargets.set(setOf(KonanTarget.IOS_ARM64))
                    transitiveSwiftPMMetadata.set(
                        TransitiveSwiftPMMetadata(
                            mapOf(
                                SwiftPMDependencyIdentifier("dep", true) to SwiftPMImportMetadata(
                                    konanTargets = setOf("ios_arm64"),
                                    iosDeploymentVersion = "123.0",
                                    macosDeploymentVersion = "234.0",
                                    watchosDeploymentVersion = null,
                                    tvosDeploymentVersion = null,
                                    isModulesDiscoveryEnabled = true,
                                    dependencies = setOf(
                                        SwiftPMDependency.Remote(
                                            repository = SwiftPMDependency.Remote.Repository.Url("https://foo.bar/baz"),
                                            version = SwiftPMDependency.Remote.Version.Exact("1.0.0"),
                                            products = listOf(
                                                SwiftPMDependency.Product("dep"),
                                            ),
                                            cinteropClangModules = emptyList(),
                                            packageName = "baz",
                                            traits = setOf()
                                        )
                                    ),
                                ),
                            )
                        )
                    )
                    syntheticProductType.set(SyntheticProductType.INFERRED)
                }
            }
            build("packageGeneration")

            val rootPackagePath = projectPath.resolve("build/kotlin/swiftImport")
            val generatedRootPackage = json.decodeFromString<PackageDescription>(
                runProcess(
                    listOf("swift", "package", "describe", "--type", "json"),
                    rootPackagePath.toFile(),
                ).output
            )
            val subpackagePath = rootPackagePath.resolve("subpackages/dep")

            assertEquals(
                PackageDescription(
                    dependencies = listOf(
                        PackageDescription.PackageDependency(
                            identity = "dep",
                            path = subpackagePath.toRealPath().pathString,
                            type = "fileSystem",
                            url = null,
                        ),
                    ),
                    platforms = listOf(
                        PackageDescription.PackagePlatform(
                            name = "ios",
                            version = "123.0",
                        ),
                    ),
                    products = listOf(
                        PackageDescription.PackageProduct(
                            name = "KotlinMultiplatformLinkedPackage",
                            targets = listOf(
                                "KotlinMultiplatformLinkedPackage",
                            ),
                            type = mapOf(
                                "library" to listOf(
                                    "automatic",
                                ),
                            ),
                        ),
                    ),
                    targets = listOf(
                        PackageDescription.PackageTarget(
                            path = "Sources/KotlinMultiplatformLinkedPackage",
                            productDependencies = listOf(
                                "dep",
                            ),
                            type = "library",
                        ),
                    ),
                ).prettyPrinted,
                generatedRootPackage.prettyPrinted,
            )

            val generatedSubpackage = json.decodeFromString<PackageDescription>(
                runProcess(
                    listOf("swift", "package", "describe", "--type", "json"),
                    subpackagePath.toFile(),
                ).output
            )
            assertEquals(
                PackageDescription(
                    dependencies = listOf(
                        PackageDescription.PackageDependency(
                            identity = "baz",
                            path = null,
                            type = "sourceControl",
                            url = "https://foo.bar/baz",
                        ),
                    ),
                    platforms = listOf(
                        PackageDescription.PackagePlatform(
                            name = "ios",
                            version = "123.0",
                        ),
                    ),
                    products = listOf(
                        PackageDescription.PackageProduct(
                            name = "dep",
                            targets = listOf(
                                "dep",
                            ),
                            type = mapOf(
                                "library" to listOf(
                                    "automatic",
                                ),
                            ),
                        ),
                    ),
                    targets = listOf(
                        PackageDescription.PackageTarget(
                            path = "Sources/dep",
                            productDependencies = listOf(
                                "dep",
                            ),
                            type = "library",
                        ),
                    ),
                ).prettyPrinted,
                generatedSubpackage.prettyPrinted,
            )
        }
    }

    @GradleTest
    fun `KT-82823 - stale subpackages are removed and generated files are read-only`(version: GradleVersion) {
        val includeStaleDependency = "includeStaleDependency"

        val syntheticImportSubpackagesDir = GenerateSyntheticLinkageImportProject.Companion.SUBPACKAGES
        val syntheticImportManifestName = GenerateSyntheticLinkageImportProject.Companion.MANIFEST_NAME

        val retainedSubpackage = "retained"
        val stateSubpackage = "stale"

        fun syntheticSubpackageDir(packageRoot: Path, identifier: String): Path =
            packageRoot.resolve(syntheticImportSubpackagesDir).resolve(identifier)

        fun generatedFiles(root: Path, identifier: String): List<Path> = listOf(
            syntheticImportManifestName,
            "Sources/$identifier/$identifier.m",
            "Sources/$identifier/include/$identifier.h",
            "Sources/$identifier/include/module.modulemap",
        ).map { root.resolve(it) }

        fun syntheticPackageGeneratedFiles(packageRoot: Path, subpackages: List<String> = emptyList()): List<Path> =
            generatedFiles(packageRoot, SYNTHETIC_IMPORT_TARGET_MAGIC_NAME) +
                    subpackages.flatMap { generatedFiles(syntheticSubpackageDir(packageRoot, it), it) }

        project("empty", version) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.createKotlinExtension(KotlinMultiplatformExtension::class)
                val extension = project.locateOrRegisterSwiftPMDependenciesExtension()
                val includeStale = project.providers.gradleProperty(includeStaleDependency).isPresent
                val subpackages = if (includeStale) {
                    listOf(retainedSubpackage, stateSubpackage)
                } else {
                    listOf(retainedSubpackage)
                }
                project.tasks.register<GenerateSyntheticLinkageImportProject>("packageGeneration") {
                    configureWithExtension(extension)
                    konanTargets.set(setOf(KonanTarget.IOS_ARM64))
                    syntheticProductType.set(SyntheticProductType.INFERRED)
                    transitiveSwiftPMMetadata.set(
                        TransitiveSwiftPMMetadata(
                            subpackages.associate { identifier ->
                                SwiftPMDependencyIdentifier(identifier, false) to SwiftPMImportMetadata(
                                    konanTargets = setOf("ios_arm64"),
                                    iosDeploymentVersion = "123.0",
                                    macosDeploymentVersion = null,
                                    watchosDeploymentVersion = null,
                                    tvosDeploymentVersion = null,
                                    isModulesDiscoveryEnabled = true,
                                    dependencies = setOf(
                                        SwiftPMDependency.Remote(
                                            repository = SwiftPMDependency.Remote.Repository.Url("https://foo.bar/$identifier"),
                                            version = SwiftPMDependency.Remote.Version.Exact("1.0.0"),
                                            products = listOf(SwiftPMDependency.Product(identifier)),
                                            cinteropClangModules = emptyList(),
                                            packageName = identifier,
                                            traits = emptySet(),
                                        )
                                    ),
                                )
                            }
                        )
                    )
                }
            }

            val packageRoot = projectPath.resolve("build/kotlin/swiftImport")

            build("packageGeneration", "-P$includeStaleDependency=true") {
                assertTasksExecuted(":packageGeneration")
                assertEquals(
                    listOf(retainedSubpackage, stateSubpackage),
                    describeSwiftPackage(packageRoot).dependencies.map { it.identity },
                    "Both subpackages should be declared in the generated manifest",
                )
                assertDirectoryExists(syntheticSubpackageDir(packageRoot, retainedSubpackage))
                assertDirectoryExists(syntheticSubpackageDir(packageRoot, stateSubpackage))

                syntheticPackageGeneratedFiles(packageRoot, listOf(retainedSubpackage, stateSubpackage))
                    .forEach { generatedFile ->
                        assertFalse(
                            generatedFile.isWritable(),
                            "Generated file '${generatedFile.relativeTo(packageRoot)}' should be read-only",
                        )
                    }
            }

            // Regenerating over the read-only files of the previous run has to work
            build("packageGeneration") {
                assertTasksExecuted(":packageGeneration")
                assertEquals(
                    listOf(retainedSubpackage),
                    describeSwiftPackage(packageRoot).dependencies.map { it.identity },
                    "The stale subpackage should be dropped from the generated manifest",
                )
                assertDirectoryExists(syntheticSubpackageDir(packageRoot, retainedSubpackage))

                assertDirectoryDoesNotExist(
                    syntheticSubpackageDir(packageRoot, stateSubpackage),
                    message = "Subpackage directory should be removed together with the dependency",
                )

                syntheticPackageGeneratedFiles(packageRoot, listOf(retainedSubpackage)).forEach { generatedFile ->
                    assertFalse(
                        generatedFile.isWritable(),
                        "Regenerated file '${generatedFile.relativeTo(packageRoot)}' should be read-only again",
                    )
                }
            }
        }
    }

    @GradleTest
    fun `generate task generates same package given the same synthetic package fingerprint`(version: GradleVersion) {
        val subProjectName = "subProject"

        project("empty", version) {
            withLockFileFixture {
                val swiftPmPackage = repoRef("Maps").also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    sourceSets.appleMain.dependencies {
                        api(project(":$subProjectName"))
                    }
                }

                val subProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(swiftPmPackage.url),
                                version = exact("1.0.0"),
                                products = listOf(product(swiftPmPackage.name))
                            )
                        }
                    }
                }
                include(subProject, subProjectName)

                val generatePackageTask = GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName


                build(
                    ":$generatePackageTask",
                    ":$subProjectName:$generatePackageTask",
                ) {
                    val subProjectFingerprintFile =
                        subProject.projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)
                    val rootProjectFingerprintFile =
                        projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)

                    assertTasksExecuted(
                        ":$generatePackageTask",
                        ":$subProjectName:$generatePackageTask",
                    )

                    val subProjectGeneratePackageHash =
                        parseSwiftPMIncrementalFingerprint(subProjectFingerprintFile)

                    val rootProjectGeneratePackageHash =
                        parseSwiftPMIncrementalFingerprint(rootProjectFingerprintFile)

                    assertEquals(
                        subProjectGeneratePackageHash,
                        rootProjectGeneratePackageHash,
                        "Projects with same flattened dependency graphs and same build settings should have same fingerprint"
                    )

                    assertFileExists(
                        projectPath.resolve("build/kotlin/swiftSyntheticPackages/$rootProjectGeneratePackageHash/Package.swift")
                    )

                    assertFileExists(
                        projectPath.resolve("build/kotlin/swiftImport/Package.swift")
                    )

                    assertFileExists(
                        subProject.projectPath.resolve("build/kotlin/swiftImport/Package.swift")
                    )
                }
            }
        }
    }


    @GradleTest
    fun `generate task generates different packages given the different target fingerprint hash`(version: GradleVersion) {
        val subProjectName = "subProject"

        project("empty", version) {
            withLockFileFixture {
                val mapsPackage = repoRef("Maps").also { createRepo(it.name, listOf("1.0.0")) }
                val crpytoPackage = repoRef("Crypto").also { createRepo(it.name, listOf("1.0.0")) }


                initSwiftPmProject(cacheDirFile) {

                    swiftPMDependencies {
                        swiftPackage(
                            url = url(crpytoPackage.url),
                            version = exact("1.0.0"),
                            products = listOf(product(crpytoPackage.name))
                        )
                    }
                    sourceSets.appleMain.dependencies {
                        api(project(":$subProjectName"))
                    }
                }

                val subProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(mapsPackage.url),
                                version = exact("1.0.0"),
                                products = listOf(product(mapsPackage.name))
                            )
                        }
                    }
                }
                include(subProject, subProjectName)

                val generatePackageTask = GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName


                build(
                    ":$generatePackageTask",
                    ":$subProjectName:$generatePackageTask",
                ) {
                    val subProjectFingerprintFile =
                        subProject.projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)
                    val rootProjectFingerprintFile =
                        projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)

                    assertTasksExecuted(
                        ":$generatePackageTask",
                        ":$subProjectName:$generatePackageTask",
                    )

                    val subProjectGeneratePackageHash =
                        parseSwiftPMIncrementalFingerprint(subProjectFingerprintFile)

                    val rootProjectGeneratePackageHash =
                        parseSwiftPMIncrementalFingerprint(rootProjectFingerprintFile)

                    assertNotEquals(
                        subProjectGeneratePackageHash,
                        rootProjectGeneratePackageHash,
                        "Projects with different flattened dependency graphs should have different fingerprint"
                    )

                    assertFileExists(
                        projectPath.resolve("build/kotlin/swiftSyntheticPackages/$rootProjectGeneratePackageHash/Package.swift")
                    )

                    assertFileExists(
                        projectPath.resolve("build/kotlin/swiftImport/Package.swift")
                    )

                    assertFileExists(
                        subProject.projectPath.resolve("build/kotlin/swiftImport/Package.swift")
                    )
                }
            }
        }
    }

    @Serializable
    data class PackageDescription(
        val dependencies: List<PackageDependency>,
        val platforms: List<PackagePlatform>,
        val products: List<PackageProduct>,
        val targets: List<PackageTarget>,
    ) {
        @Serializable
        data class PackageProduct(
            val name: String,
            val targets: List<String>,
            val type: Map<String, List<String>>,
        )

        @Serializable
        data class PackageDependency(
            val identity: String,
            val type: String,
            val url: String? = null,
            val path: String? = null,
        )


        @Serializable
        data class PackagePlatform(
            val name: String,
            val version: String,
        )

        @Serializable
        data class PackageTarget(
            val path: String,
            val type: String,
            @kotlinx.serialization.SerialName("product_dependencies") val productDependencies: List<String>,
        )
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
