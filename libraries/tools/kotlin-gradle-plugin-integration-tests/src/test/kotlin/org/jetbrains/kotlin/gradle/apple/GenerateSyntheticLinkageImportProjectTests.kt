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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.SyntheticProductType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependencyIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.TransitiveSwiftPMDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.condition.OS
import kotlin.String
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString
import kotlin.test.assertEquals

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
            runProcess(["swift", "package", "init", "--type", "library"], packageOne)

            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.createKotlinExtension(KotlinMultiplatformExtension::class)
                val extension = project.locateOrRegisterSwiftPMDependenciesExtension().apply {
                    iosMinimumDeploymentTarget.set("123.0")
                    tvosMinimumDeploymentTarget.set("234.0")
                    swiftPackage("https://foo.bar/baz.git", "1.0.0", ["a", "b"])
                    localSwiftPackage(project.layout.projectDirectory.dir("packageOne"), ["packageOne"])
                }
                project.tasks.register<GenerateSyntheticLinkageImportProject>("packageGeneration") {
                    configureWithExtension(extension)
                    konanTargets.set(setOf(KonanTarget.IOS_ARM64))
                    dependencyIdentifierToImportedSwiftPMDependencies.set(TransitiveSwiftPMDependencies(emptyMap()))
                    syntheticProductType.set(SyntheticProductType.INFERRED)
                }
            }
            build("packageGeneration")

            val generatedPackage = json.decodeFromString<PackageDescription>(
                runProcess(
                    ["swift", "package", "describe", "--type", "json"],
                    projectPath.resolve("build/kotlin/swiftImport").toFile(),
                ).output
            )

            assertEquals(
                PackageDescription(
                    dependencies = [
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
                    ],
                    platforms = [
                        PackageDescription.PackagePlatform(
                            name = "ios",
                            version = "123.0",
                        ),
                    ],
                    products = [
                        PackageDescription.PackageProduct(
                            name = "KotlinMultiplatformLinkedPackage",
                            targets = [
                                "KotlinMultiplatformLinkedPackage",
                            ],
                            type = mapOf(
                                "library" to [
                                    "automatic",
                                ],
                            ),
                        ),
                    ],
                    targets = [
                        PackageDescription.PackageTarget(
                            path = "Sources/KotlinMultiplatformLinkedPackage",
                            productDependencies = [
                                "a",
                                "b",
                                "packageOne",
                            ],
                            type = "library",
                        ),
                    ],
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
                    dependencyIdentifierToImportedSwiftPMDependencies.set(
                        TransitiveSwiftPMDependencies(
                            mapOf(
                                SwiftPMDependencyIdentifier("dep", true) to SwiftPMImportMetadata(
                                    konanTargets = ["ios_arm64"],
                                    iosDeploymentVersion = "123.0",
                                    macosDeploymentVersion = "234.0",
                                    watchosDeploymentVersion = null,
                                    tvosDeploymentVersion = null,
                                    isModulesDiscoveryEnabled = true,
                                    dependencies = [
                                        SwiftPMDependency.Remote(
                                            repository = SwiftPMDependency.Remote.Repository.Url("https://foo.bar/baz"),
                                            version = SwiftPMDependency.Remote.Version.Exact("1.0.0"),
                                            products = [
                                                SwiftPMDependency.Product("dep"),
                                            ],
                                            cinteropClangModules = [],
                                            packageName = "baz",
                                            traits = []
                                        )
                                    ],
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
                    ["swift", "package", "describe", "--type", "json"],
                    rootPackagePath.toFile(),
                ).output
            )
            val subpackagePath = rootPackagePath.resolve("subpackages/dep")

            assertEquals(
                PackageDescription(
                    dependencies = [
                        PackageDescription.PackageDependency(
                            identity = "dep",
                            path = subpackagePath.toRealPath().pathString,
                            type = "fileSystem",
                            url = null,
                        ),
                    ],
                    platforms = [
                        PackageDescription.PackagePlatform(
                            name = "ios",
                            version = "123.0",
                        ),
                    ],
                    products = [
                        PackageDescription.PackageProduct(
                            name = "KotlinMultiplatformLinkedPackage",
                            targets = [
                                "KotlinMultiplatformLinkedPackage",
                            ],
                            type = mapOf(
                                "library" to [
                                    "automatic",
                                ],
                            ),
                        ),
                    ],
                    targets = [
                        PackageDescription.PackageTarget(
                            path = "Sources/KotlinMultiplatformLinkedPackage",
                            productDependencies = [
                                "dep",
                            ],
                            type = "library",
                        ),
                    ],
                ).prettyPrinted,
                generatedRootPackage.prettyPrinted,
            )

            val generatedSubpackage = json.decodeFromString<PackageDescription>(
                runProcess(
                    ["swift", "package", "describe", "--type", "json"],
                    subpackagePath.toFile(),
                ).output
            )
            assertEquals(
                PackageDescription(
                    dependencies = [
                        PackageDescription.PackageDependency(
                            identity = "baz",
                            path = null,
                            type = "sourceControl",
                            url = "https://foo.bar/baz",
                        ),
                    ],
                    platforms = [
                        PackageDescription.PackagePlatform(
                            name = "ios",
                            version = "123.0",
                        ),
                    ],
                    products = [
                        PackageDescription.PackageProduct(
                            name = "dep",
                            targets = [
                                "dep",
                            ],
                            type = mapOf(
                                "library" to [
                                    "automatic",
                                ],
                            ),
                        ),
                    ],
                    targets = [
                        PackageDescription.PackageTarget(
                            path = "Sources/dep",
                            productDependencies = [
                                "dep",
                            ],
                            type = "library",
                        ),
                    ],
                ).prettyPrinted,
                generatedSubpackage.prettyPrinted,
            )
        }
    }

    @Serializable
    data class PackageDescription(
        val dependencies: List<PackageDependency>,
        val platforms: List<PackagePlatform>,
        val products: List<PackageProduct>,
        val targets: List<PackageTarget>
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
