/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.PublishedProject
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.jetbrains.kotlin.gradle.util.isTeamCityRun
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.test.*

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@OptIn(EnvironmentalVariablesOverride::class)
@DisplayName("SwiftPM import Xcode integration tests")
@SwiftPMImportGradlePluginTests
class SwiftPMImportXcodeIntegrationIT : KGPBaseTest() {

    @GradleTest
    fun `integrateLinkagePackage task creates synthetic package`(version: GradleVersion) {
        project("emptyxcode", version) {
            initDefaultKmpWithLocalSPM()

            val pbxFile = projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")
            val manifestFile = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift")

            assertFileDoesNotContain(
                pbxFile,
                SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
            )

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                assertTasksExecuted(":generateSyntheticLinkageSwiftPMImportProjectForLinkageForCli")
                assertXcodeBuildDependencyChain(projectPath.resolve("iosApp"))

                val pbxFileContent = pbxFile.readText()
                assertTrue(
                    projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME").exists(),
                    "$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME should be created"
                )
                assertContains(
                    pbxFileContent,
                    "relativePath = $SYNTHETIC_IMPORT_TARGET_MAGIC_NAME;",
                    message = "Local package reference should point to the synthetic package path"
                )
                assertContains(
                    pbxFileContent,
                    "productName = $SYNTHETIC_IMPORT_TARGET_MAGIC_NAME;",
                    message = "Swift package dependency should reference the synthetic product name"
                )

                assertEquals(
                    SwiftPackageLibraryType.AUTOMATIC,
                    describeSwiftPackage(manifestFile.parent).products.first().type.library?.first(),
                    message = "Synthetic package product type should be 'automatic' ('.none' in package.swift) when isStatic=true"
                )
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage sets dynamic synthetic product type for dynamic frameworks`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localSwiftPackageRelativePath = "../localSwiftPackage"
            createLocalSwiftPackage(projectPath.resolve(localSwiftPackageRelativePath))

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = false
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localSwiftPackageRelativePath),
                            products = listOf("LocalSwiftPackage"),
                        )
                    }
                }
            }

            val manifestFile = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift")

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                assertTrue(
                    manifestFile.exists(),
                    "Synthetic Package.swift should be generated"
                )
                assertEquals(
                    SwiftPackageLibraryType.DYNAMIC,
                    describeSwiftPackage(manifestFile.parent).products.first().type.library?.first(),
                    message = "Synthetic package product type should be '.dynamic' when isStatic=false"
                )
                // https://youtrack.jetbrains.com/issue/KT-82824/Make-linker-hack-path-relative
                assertEquals(
                    "-fuse-ld=${projectPath.toRealPath().absolutePathString()}/iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/linkerHack",
                    dumpSwiftPackage(manifestFile.parent).getFirstUnsafeFlag(),
                    message = "Package.swift should have linker hack when isStatic=false"
                )
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage passes non-default iosDeploymentVersion into synthetic manifest`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localSwiftPackageRelativePath = "../localSwiftPackage"
            createLocalSwiftPackage(projectPath.resolve(localSwiftPackageRelativePath))

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64(),
                        macosArm64(),
                        tvosArm64(),
                        watchosArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        iosMinimumDeploymentTarget.set("16.4")
                        macosMinimumDeploymentTarget.set("14.6")
                        watchosMinimumDeploymentTarget.set("11.6")
                        tvosMinimumDeploymentTarget.set("18.6")
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localSwiftPackageRelativePath),
                            products = listOf("LocalSwiftPackage"),
                        )
                    }
                }
            }

            val manifestFile = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift")

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                assertTrue(
                    manifestFile.exists(),
                    "Synthetic Package.swift should be generated"
                )
                val packageDescription = describeSwiftPackage(manifestFile.parent)
                val platforms = packageDescription.platforms.associateBy { it.name }

                assertEquals(
                    "16.4",
                    platforms["ios"]?.version,
                    message = "Synthetic Package.swift should contain explicitly configured iOS deployment version"
                )
                assertEquals(
                    "14.6",
                    platforms["macos"]?.version,
                    message = "Synthetic Package.swift should contain explicitly configured macOS deployment version"
                )
                assertEquals(
                    "18.6",
                    platforms["tvos"]?.version,
                    message = "Synthetic Package.swift should contain explicitly configured tvOS deployment version"
                )
                assertEquals(
                    "11.6",
                    platforms["watchos"]?.version,
                    message = "Synthetic Package.swift should contain explicitly configured watchOS deployment version"
                )
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage passes different version types correctly`(version: GradleVersion) {
        project("emptyxcode", version) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        swiftPackage(
                            url = url("https://github.com/apple/swift-protobuf-exact.git"),
                            version = exact("1.32.0-exact"),
                            products = listOf(),
                        )
                        swiftPackage(
                            url = "https://github.com/apple/swift-protobuf-string.git",
                            version = "1.32.0-string",
                            products = listOf(),
                        )
                        swiftPackage(
                            url = url("https://github.com/apple/swift-protobuf-from.git"),
                            version = from("1.32.0-from"),
                            products = listOf(),
                        )
                        swiftPackage(
                            url = url("https://github.com/apple/swift-protobuf-range.git"),
                            version = range("1.32.0-range1", "1.32.0-range2"),
                            products = listOf(),
                        )
                        swiftPackage(
                            url = url("https://github.com/apple/swift-protobuf-branch.git"),
                            version = branch("git-branch"),
                            products = listOf(),
                        )
                        swiftPackage(
                            url = url("https://github.com/apple/swift-protobuf-revision.git"),
                            version = revision("git-revision"),
                            products = listOf(),
                        )
                    }
                }
            }

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                val manifestContent = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift").readText()

                assertContains(manifestContent, "exact: \"1.32.0-exact\"")
                assertContains(manifestContent, "from: \"1.32.0-string\"")
                assertContains(manifestContent, "from: \"1.32.0-from\"")
                assertContains(manifestContent, "\"1.32.0-range1\"...\"1.32.0-range2\"")
                assertContains(manifestContent, "branch: \"git-branch\"")
                assertContains(manifestContent, "revision: \"git-revision\"")
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage passes product platform constraints into synthetic manifest`(version: GradleVersion) {
        project("emptyxcode", version) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        swiftPackage(
                            url = url("https://github.com/aws-amplify/aws-sdk-ios-spm.git"),
                            version = from("2.41.0"),
                            products = listOf(
                                product("AWSS3", platforms = setOf(iOS())),
                                product("AWSEC2", platforms = setOf(macOS(), tvOS())),
                                product("AWSMobileClient", platforms = setOf(watchOS())),
                                product("AWSCore"),
                            ),
                        )
                    }
                }
            }

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                // Normalize whitespace to make multi-line manifest easier to check
                val manifestContent = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift")
                    .readText()
                    .replace(Regex("\\s+"), " ")

                assertContains(
                    manifestContent, ".product( name: \"AWSS3\", package: \"aws-sdk-ios-spm\", condition: .when(platforms: [.iOS]), ),",
                    message = "AWSS3 product should have iOS platform condition"
                )
                assertContains(
                    manifestContent,
                    ".product( name: \"AWSEC2\", package: \"aws-sdk-ios-spm\", condition: .when(platforms: [.macOS, .tvOS]), ),",
                    message = "AWSEC2 product should have macOS and tvOS platform conditions"
                )
                assertContains(
                    manifestContent,
                    ".product( name: \"AWSMobileClient\", package: \"aws-sdk-ios-spm\", condition: .when(platforms: [.watchOS]), ),",
                    message = "AWSMobileClient product should have watchOS platform condition"
                )
                assertContains(
                    manifestContent, ".product( name: \"AWSCore\", package: \"aws-sdk-ios-spm\", )",
                    message = "AWSCore product should have no platform condition"
                )
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage task base idempotency check`(version: GradleVersion) {
        project("emptyxcode", version, buildOptions = defaultBuildOptions.disableConfigurationCacheForGradle7(version)) {
            initDefaultKmpWithLocalSPM()

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            )
            val pbxFile = projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")
            val pbxFileContentBeforeTheSecondRun = pbxFile.readText()
            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                assertEquals(pbxFileContentBeforeTheSecondRun, pbxFile.readText())
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage reruns synthetic manifest generation when new Package is added`(version: GradleVersion) {
        project("emptyxcode", version) {
            val includeSecondPackageProp = "includeSecondPackage"
            val localSwiftPackageRelativePath = "../localSwiftPackage"
            val secondLocalSwiftPackageRelativePath = "../secondLocalSwiftPackage"
            createLocalSwiftPackage(projectPath.resolve(localSwiftPackageRelativePath))
            createLocalSwiftPackage(projectPath.resolve(secondLocalSwiftPackageRelativePath), packageName = "SecondLocalSwiftPackage")

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localSwiftPackageRelativePath),
                            products = listOf("LocalSwiftPackage"),
                        )
                        if (project.hasProperty(includeSecondPackageProp)) {
                            localSwiftPackage(
                                directory = project.layout.projectDirectory.dir(secondLocalSwiftPackageRelativePath),
                                products = listOf("SecondLocalSwiftPackage"),
                            )
                            swiftPackage(
                                url = url("https://github.com/apple/swift-protobuf.git"),
                                version = exact("1.32.0"),
                                products = listOf(),
                            )
                        }
                    }
                }
            }

            val manifestFile = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift")

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                val dependencies = describeSwiftPackage(manifestFile.parent).dependencies
                assertEquals(
                    listOf("localswiftpackage"),
                    dependencies.map { it.identity },
                    message = "synthetic package depends only on LocalSwiftPackage"
                )
            }

            build(
                "integrateLinkagePackage", "-P${includeSecondPackageProp}=true",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                val dependencies = describeSwiftPackage(manifestFile.parent).dependencies
                assertEquals(
                    listOf("localswiftpackage", "secondlocalswiftpackage", "swift-protobuf"),
                    dependencies.map { it.identity },
                    message = "synthetic package depends on LocalSwiftPackage and SecondLocalSwiftPackage and swift-protobuf"
                )
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage reruns synthetic manifest generation when Package is removed`(version: GradleVersion) {
        project("emptyxcode", version) {
            val includeSecondPackageProp = "includeSecondPackage"
            val localSwiftPackageRelativePath = "../localSwiftPackage"
            createLocalSwiftPackage(projectPath.resolve(localSwiftPackageRelativePath))

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localSwiftPackageRelativePath),
                            products = listOf("LocalSwiftPackage"),
                        )
                        if (project.hasProperty(includeSecondPackageProp)) {
                            swiftPackage(
                                url = url("https://github.com/apple/swift-protobuf.git"),
                                version = exact("1.32.0"),
                                products = listOf(),
                            )
                        }
                    }
                }
            }

            val manifestFile = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift")

            build(
                "integrateLinkagePackage", "-P${includeSecondPackageProp}=true",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                val dependencies = describeSwiftPackage(manifestFile.parent).dependencies
                assertEquals(
                    listOf("localswiftpackage", "swift-protobuf"),
                    dependencies.map { it.identity },
                    message = "synthetic package depends on LocalSwiftPackage and swift-protobuf"
                )
            }

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                assertTasksExecuted(":integrateLinkagePackage")

                val dependencies = describeSwiftPackage(manifestFile.parent).dependencies
                assertEquals(
                    listOf("localswiftpackage"),
                    dependencies.map { it.identity },
                    message = "synthetic package depends only on LocalSwiftPackage"
                )
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage cleanup subpackage dir after removing dependency`(version: GradleVersion) {
        val includeSubprojectB = "includeSubprojectB"
        project("emptyxcode", version) {
            initDefaultKmpWithLocalSPM {
                sourceSets.commonMain.dependencies {
                    api(project(":subprojectA"))
                    if (project.hasProperty(includeSubprojectB)) {
                        api(project(":subprojectB"))
                    }
                }
            }

            val subprojectA = project("empty", version) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        iosSimulatorArm64()

                        swiftPMDependencies {
                            swiftPackage(
                                url = url("https://github.com/apple/swift-protobuf-subprojectA.git"),
                                version = exact("1.32.0"),
                                products = listOf(),
                            )
                        }
                    }
                }
            }

            val subprojectB = project("empty", version) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        iosSimulatorArm64()

                        swiftPMDependencies {
                            swiftPackage(
                                url = url("https://github.com/apple/swift-protobuf-subprojectB.git"),
                                version = exact("1.32.0"),
                                products = listOf(),
                            )
                        }
                    }
                }
            }

            include(subprojectA, "subprojectA")
            include(subprojectB, "subprojectB")

            build(
                "integrateLinkagePackage", "-P$includeSubprojectB=true",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                val manifestFile = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift")
                val manifestDependencies = describeSwiftPackage(manifestFile.parent).dependencies

                assertEquals(
                    listOf("localswiftpackage", "_subprojecta", "_subprojectb"),
                    manifestDependencies.map { it.identity },
                    message = "Manifest should contain subprojectA and subprojectB dependencies"
                )

                assertTrue(
                    projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/subpackages/_subprojectA").exists(),
                    message = "Subpackage directory for subprojectA should exist"
                )

                assertTrue(
                    projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/subpackages/_subprojectB").exists(),
                    message = "Subpackage directory for subprojectB should exist"
                )
            }

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                val manifestFile = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift")
                val manifestDependencies = describeSwiftPackage(manifestFile.parent).dependencies

                assertEquals(
                    listOf("localswiftpackage", "_subprojecta"),
                    manifestDependencies.map { it.identity },
                    message = "Manifest should contain subprojectA dependency"
                )

                assertTrue(
                    projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/subpackages/_subprojectA").exists(),
                    message = "Subpackage directory for subprojectA should exist"
                )

                // https://youtrack.jetbrains.com/issue/KT-82823
                assertTrue(
                    projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/subpackages/_subprojectB").exists(),
                    message = "Subpackage directory for subprojectB exists, should be fixed in KT-82823"
                )
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage collects dependencies from multiple subprojects`(version: GradleVersion) {
        project("emptyxcode", version) {
            initDefaultKmpWithLocalSPM {
                sourceSets.commonMain.dependencies {
                    api(project(":subprojectA"))
                    api(project(":subprojectB"))
                }
            }

            val subprojectA = project("empty", version) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        iosSimulatorArm64()

                        swiftPMDependencies {
                            swiftPackage(
                                url = url("https://github.com/apple/swift-protobuf-subprojectA.git"),
                                version = exact("1.32.0"),
                                products = listOf(),
                            )
                        }
                    }
                }
            }

            val subprojectB = project("empty", version) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        iosSimulatorArm64()

                        swiftPMDependencies {
                            swiftPackage(
                                url = url("https://github.com/apple/swift-protobuf-subprojectB.git"),
                                version = exact("1.32.0"),
                                products = listOf(),
                            )
                        }
                    }
                }
            }

            include(subprojectA, "subprojectA")
            include(subprojectB, "subprojectB")

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                val manifest = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift").readText()

                assertContains(
                    manifest,
                    "path: \"../../../localSwiftPackage\"",
                    message = "Manifest should contain localSwiftPackage from root project"
                )

                assertContains(
                    manifest,
                    ".package(path: \"subpackages/_subprojectA\")",
                    message = "Manifest should contain subprojectA from root project"
                )

                assertContains(
                    manifest,
                    ".package(path: \"subpackages/_subprojectB\")",
                    message = "Manifest should contain subprojectB from root project"
                )

                assertContains(
                    projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/subpackages/_subprojectA/Package.swift").readText(),
                    "https://github.com/apple/swift-protobuf-subprojectA.git",
                    message = "Manifest should contain subprojectA from root project"
                )

                assertContains(
                    projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/subpackages/_subprojectB/Package.swift").readText(),
                    "https://github.com/apple/swift-protobuf-subprojectB.git",
                    message = "Manifest should contain subprojectB from root project"
                )
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage with transitive published dependency`(version: GradleVersion) {
        val producer = project("empty", version) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()

                    swiftPMDependencies {
                        swiftPackage(
                            url = url("https://github.com/apple/swift-protobuf.git"),
                            version = exact("1.32.0"),
                            products = listOf(),
                        )
                    }
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "dependency"))

        project("emptyxcode", version) {
            plugins {
                kotlin("multiplatform")
            }

            addPublishedProjectToRepositories(producer)

            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    sourceSets.commonMain.dependencies {
                        api(producer.rootCoordinate)
                    }
                }
            }

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                val rootManifest = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift")
                assertTrue(rootManifest.exists(), "synthetic project should be created")

                val rootManifestDependencies = describeSwiftPackage(rootManifest.parent).dependencies
                assertEquals(
                    producer.spmRootCoordinates(),
                    rootManifestDependencies.first().identity,
                    "published dependency should be added to the manifest"
                )

                val subpackageManifest =
                    projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/subpackages/${producer.spmRootCoordinates()}/Package.swift")
                val subpackageManifestDependencies = describeSwiftPackage(subpackageManifest.parent).dependencies

                assertEquals(
                    "https://github.com/apple/swift-protobuf.git",
                    subpackageManifestDependencies.first().url,
                    "published dependency should be added to the manifest"
                )
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage with transitive published dependency through project dependency`(version: GradleVersion) {
        val transitive = project("empty", version) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()

                    swiftPMDependencies {
                        swiftPackage(
                            url = url("https://github.com/apple/swift-protobuf.git"),
                            version = exact("1.32.0"),
                            products = listOf(),
                        )
                    }
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "dependency"))

        project("emptyxcode", version) {
            plugins {
                kotlin("multiplatform")
            }

            addPublishedProjectToRepositories(transitive)

            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    sourceSets.commonMain.dependencies {
                        implementation(project(":direct"))
                    }
                }
            }

            val direct = project("empty", version) {
                plugins {
                    kotlin("multiplatform")
                }

                addPublishedProjectToRepositories(transitive)
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        iosSimulatorArm64()

                        sourceSets.commonMain.dependencies {
                            api(transitive.rootCoordinate)
                        }
                    }
                }
            }
            include(direct, "direct", useSymlink = false)

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                val rootManifest = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift")
                assertTrue(rootManifest.exists(), "synthetic project should be created")

                val rootManifestDependencies = describeSwiftPackage(rootManifest.parent).dependencies
                assertEquals(
                    transitive.spmRootCoordinates(),
                    rootManifestDependencies.first().identity,
                    "published dependency should be added to the manifest"
                )

                val subpackageManifest =
                    projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/subpackages/${transitive.spmRootCoordinates()}/Package.swift")
                val subpackageManifestDependencies = describeSwiftPackage(subpackageManifest.parent).dependencies

                assertEquals(
                    "https://github.com/apple/swift-protobuf.git",
                    subpackageManifestDependencies.first().url,
                    "published dependency should be added to the manifest"
                )
            }
        }
    }

    @Ignore("TODO: investigate why packageRoot and SPM absolute path a differ")
    @GradleTest
    fun `integrateLinkagePackage with non root project`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localSwiftPackageRelativePath = "localSwiftPackage"
            createLocalSwiftPackage(projectPath.resolve(localSwiftPackageRelativePath))

            plugins {
                kotlin("multiplatform").apply(false)
            }

            val subprojectA = project("empty", version) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        listOf(
                            iosArm64(),
                            iosSimulatorArm64()
                        ).forEach {
                            it.binaries.framework {
                                baseName = "Shared"
                                isStatic = true
                            }
                        }

                        sourceSets.commonMain.dependencies {
                            api(project(":subprojectB"))
                        }

                        swiftPMDependencies {
                            localSwiftPackage(
                                directory = project.layout.projectDirectory.dir("../$localSwiftPackageRelativePath"),
                                products = listOf("LocalSwiftPackage"),
                            )
                        }
                    }
                }
            }

            val subprojectB = project("empty", version) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        iosSimulatorArm64()

                        swiftPMDependencies {
                            swiftPackage(
                                url = url("https://github.com/apple/swift-protobuf.git"),
                                version = exact("1.32.0"),
                                products = listOf(),
                            )
                        }
                    }
                }
            }

            include(subprojectA, "subprojectA", useSymlink = false)
            include(subprojectB, "subprojectB", useSymlink = false)

            val iosAppDir = projectPath.resolve("iosApp/iosApp.xcodeproj")
            iosAppDir.resolve("project.pbxproj")
                .replaceText(":embedAndSignAppleFrameworkForXcode", ":subprojectA:embedAndSignAppleFrameworkForXcode")

            build(
                ":subprojectA:integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to iosAppDir.absolutePathString(),
                )
            ) {

                assertTrue(
                    projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME").exists(),
                    "Expected to find created synthetic target directory"
                )

                val manifest = projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift").readText()

                assertContains(
                    manifest,
                    "path: \"../$localSwiftPackageRelativePath\"",
                    message = "Manifest should contain localSwiftPackage from root project level"
                )

                assertContains(
                    manifest,
                    ".package(path: \"subpackages/_subprojectB\")",
                    message = "Manifest should contain subprojectB from root project"
                )

                assertContains(
                    projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/subpackages/_subprojectB/Package.swift").readText(),
                    "https://github.com/apple/swift-protobuf.git",
                    message = "Manifest should contain subprojectB dependency"
                )
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage accepts absolute XCODEPROJ_PATH`(version: GradleVersion) {
        project("emptyxcode", version) {
            initDefaultKmpWithLocalSPM()

            val pbxFile = projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")
            val absoluteXcodeprojPath = projectPath.resolve("iosApp/iosApp.xcodeproj").toAbsolutePath().toString()
            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to absoluteXcodeprojPath,
                )
            ) {
                val pbxFileContent = pbxFile.readText()
                assertTrue(projectPath.resolve("iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME").exists())
                assertContains(pbxFileContent, "relativePath = $SYNTHETIC_IMPORT_TARGET_MAGIC_NAME;")
                assertContains(pbxFileContent, "productName = $SYNTHETIC_IMPORT_TARGET_MAGIC_NAME;")
            }
        }
    }

    @GradleTest
    fun `integrateLinkagePackage fails when embed-and-sign phase is absent`(version: GradleVersion) {
        project("emptyxcode-no-embedandsign", version) {
            initDefaultKmpWithLocalSPM()

            buildAndFail(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                )
            ) {
                assertOutputContains("embedAndSign integration wasn't found")
            }
        }
    }

    @Ignore("Till the fix https://youtrack.jetbrains.com/issue/KT-84384/")
    @GradleTest
    fun `integrateLinkagePackage handles symlinked DEVELOPER_DIR`(version: GradleVersion) {
        if (!isTeamCityRun) {
            Assumptions.assumeTrue(version >= GradleVersion.version("8.0"))
        }

        project("emptyxcode", version) {
            initDefaultKmpWithLocalSPM()

            val symlinkedDeveloperDir = createSymlinkedDeveloperDir(projectPath)
            val xcodeSelectOutput = runProcess(
                cmd = listOf("xcode-select", "-p"),
                workingDir = projectPath.toFile(),
                environmentVariables = mapOf(
                    "DEVELOPER_DIR" to symlinkedDeveloperDir.toString()
                )
            )
            assertEquals(0, xcodeSelectOutput.exitCode)
            assertEquals(symlinkedDeveloperDir.toString(), xcodeSelectOutput.output.trim())

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "DEVELOPER_DIR" to symlinkedDeveloperDir.toString(),
                )
            ) {
                assertTasksExecuted(":integrateLinkagePackage")
            }
        }
    }
}

private fun createSymlinkedDeveloperDir(projectPath: Path): Path {
    val xcodeSelectOutput = runProcess(
        cmd = listOf("xcode-select", "-p"),
        workingDir = projectPath.toFile(),
    )
    assertEquals(0, xcodeSelectOutput.exitCode)
    val selectedDeveloperDir = Paths.get(xcodeSelectOutput.output.trim()).toRealPath()
    val symlinkedDeveloperDir = projectPath.resolve("build/tmp/symlinkedDeveloperDir")

    symlinkedDeveloperDir.parent.createDirectories()
    symlinkedDeveloperDir.deleteIfExists()
    symlinkedDeveloperDir.createSymbolicLinkPointingTo(selectedDeveloperDir)

    return symlinkedDeveloperDir
}

private fun TestProject.initDefaultKmpWithLocalSPM(extra: KotlinMultiplatformExtension.() -> Unit = {}) {
    val localSwiftPackageRelativePath = "../localSwiftPackage"
    createLocalSwiftPackage(projectPath.resolve(localSwiftPackageRelativePath))

    plugins {
        kotlin("multiplatform")
    }
    buildScriptInjection {
        project.applyMultiplatform {
            listOf(
                iosArm64(),
                iosSimulatorArm64()
            ).forEach {
                it.binaries.framework {
                    baseName = "Shared"
                    isStatic = true
                }
            }

            swiftPMDependencies {
                localSwiftPackage(
                    directory = project.layout.projectDirectory.dir(localSwiftPackageRelativePath),
                    products = listOf("LocalSwiftPackage"),
                )
            }

            extra()
        }
    }
}

private fun SwiftPackageDump.getFirstUnsafeFlag() =
    targets.first().settings.first().kind.unsafeFlags?.flags?.first()

private fun assertXcodeBuildDependencyChain(iosAppPath: Path) {
    val pifFileTargets = dumpXcodebuildPIF(iosAppPath).filter { it.type == "target" }
    val iosAppTarget = pifFileTargets.single { it.contents.name == "iosApp" }

    assertEquals(
        listOf("PACKAGE-PRODUCT:$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME"),
        iosAppTarget.contents.dependencies.map { it.guid },
        message = "iosApp target should depend on synthetic package product"
    )

    val syntheticPackageProduct = pifFileTargets.single { it.contents.guid == "PACKAGE-PRODUCT:$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME" }
    assertEquals(
        listOf("PACKAGE-TARGET:$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME", "PACKAGE-PRODUCT:LocalSwiftPackage"),
        syntheticPackageProduct.contents.dependencies.map { it.guid },
        message = "Synthetic package product should depend on synthetic package target and LocalSwiftPackage product"
    )

    val syntheticPackageTarget = pifFileTargets.single { it.contents.guid == "PACKAGE-TARGET:$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME" }
    assertEquals(
        listOf("PACKAGE-PRODUCT:LocalSwiftPackage"),
        syntheticPackageTarget.contents.dependencies.map { it.guid },
        message = "Synthetic package target should depend on LocalSwiftPackage product"
    )

    val localSwiftPackageProduct = pifFileTargets.single { it.contents.guid == "PACKAGE-PRODUCT:LocalSwiftPackage" }
    assertEquals(
        listOf("PACKAGE-TARGET:LocalSwiftPackage"),
        localSwiftPackageProduct.contents.dependencies.map { it.guid },
        message = "LocalSwiftPackage product should depend on LocalSwiftPackage target"
    )

    val localSwiftPackageTarget = pifFileTargets.single { it.contents.guid == "PACKAGE-TARGET:LocalSwiftPackage" }
    assertEquals(
        emptyList(),
        localSwiftPackageTarget.contents.dependencies.map { it.guid },
        message = "LocalSwiftPackage target should not depend on anything"
    )
}

private fun PublishedProject.spmRootCoordinates() = rootCoordinate.replace(":", "_").replace(".", "_")