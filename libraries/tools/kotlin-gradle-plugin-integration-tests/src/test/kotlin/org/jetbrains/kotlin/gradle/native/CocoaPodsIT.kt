/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.DUMMY_FRAMEWORK_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_IMPORT_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_SPEC_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.SYNC_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.CocoapodsPluginDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.removingTrailingNewline
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.util.zip.ZipFile
import kotlin.io.path.*
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("CocoaPods plugin tests")
@NativeGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
@OptIn(EnvironmentalVariablesOverride::class)
class CocoaPodsIT : KGPBaseTest() {

    private val podfileImportPodPlaceholder = "#import_pod_directive"

    private val cocoapodsSingleKtPod = "native-cocoapods-single"
    private val cocoapodsMultipleKtPods = "native-cocoapods-multiple"
    private val cocoapodsTestsProjectName = "native-cocoapods-tests"
    private val cocoapodsCommonizationProjectName = "native-cocoapods-commonization"
    private val cocoapodsDependantPodsProjectName = "native-cocoapods-dependant-pods"

    private val dummyTaskName = ":$DUMMY_FRAMEWORK_TASK_NAME"
    private val podspecTaskName = ":$POD_SPEC_TASK_NAME"
    private val podImportTaskName = ":$POD_IMPORT_TASK_NAME"
    private val podInstallTaskName = ":${KotlinCocoapodsPlugin.POD_INSTALL_TASK_NAME}"
    private val syncTaskName = ":$SYNC_TASK_NAME"

    private val defaultPodName = "AFNetworking"

    @BeforeAll
    fun setUp() {
        ensureCocoapodsInstalled()
    }

    @DisplayName("Pod import single")
    @GradleTest
    fun testPodImportSingle(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(cocoapodsSingleKtPod, gradleVersion) {

            buildWithCocoapodsWrapper(podImportTaskName) {
                podImportAsserts(buildGradleKts)
            }

            buildWithCocoapodsWrapper(":kotlin-library:podImport") {
                podImportAsserts(subProject("kotlin-library").buildGradleKts, "kotlin-library")
            }
        }
    }

    @DisplayName("Pod import single noPodspec")
    @GradleTest
    fun testPodImportSingleNoPodspec(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(cocoapodsSingleKtPod, gradleVersion) {

            buildGradleKts.addCocoapodsBlock("noPodspec()")

            buildWithCocoapodsWrapper(podImportTaskName) {
                podImportAsserts(buildGradleKts)
            }

            buildWithCocoapodsWrapper(":kotlin-library:podImport") {
                podImportAsserts(subProject("kotlin-library").buildGradleKts, "kotlin-library")
            }
        }
    }

    @DisplayName("Pod import multiple")
    @GradleTest
    fun testPodImportMultiple(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(cocoapodsMultipleKtPods, gradleVersion) {

            buildWithCocoapodsWrapper(podImportTaskName) {
                podImportAsserts(buildGradleKts)
            }

            buildWithCocoapodsWrapper(":kotlin-library:podImport") {
                podImportAsserts(subProject("kotlin-library").buildGradleKts, "kotlin-library")
                if (gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_7_6)) {
                    assertOutputContains("Podfile location is set")
                }
            }

            buildWithCocoapodsWrapper(":second-library:podImport") {
                podImportAsserts(subProject("second-library").buildGradleKts, "second-library")
            }
        }
    }

    @DisplayName("Build with error if project version is not specified for cocoapods")
    @GradleTest
    fun errorIfVersionIsNotSpecified(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            val filteredBuildScript = buildGradleKts.useLines { lines ->
                lines.filter { line -> "version = \"1.0\"" !in line }.joinToString(separator = "\n")
            }
            buildGradleKts.writeText(filteredBuildScript)

            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsGenerateWrapper = true
                )
            )

            buildAndFail(POD_IMPORT_TASK_NAME, buildOptions = buildOptions) {
                assertOutputContains("Cocoapods Integration requires pod version to be specified.")
            }
        }
    }

    @DisplayName("Dummy UTD")
    @GradleTest
    fun testDummyUTD(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(
            nativeOptions = defaultBuildOptions.nativeOptions.copy(
                cocoapodsPlatform = "iphonesimulator",
                cocoapodsArchs = "x86_64",
                cocoapodsConfiguration = "Debug"
            )
        )

        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion, buildOptions = buildOptions) {

            buildGradleKts.addCocoapodsBlock(
                """
                    framework {
                        baseName = "shared"
                        isStatic = false
                    }
                """.trimIndent()
            )

            buildWithCocoapodsWrapper(dummyTaskName) {
                assertDirectoryInProjectExists("build/cocoapods/framework/shared.framework")
                assertDirectoryInProjectExists("build/cocoapods/framework/shared.framework.dSYM")
                assertTasksExecuted(dummyTaskName)
            }

            buildWithCocoapodsWrapper(dummyTaskName) {
                assertTasksUpToDate(dummyTaskName)
            }

            buildWithCocoapodsWrapper(syncTaskName) {
                assertTasksExecuted(syncTaskName)
            }

            val frameworkResult = runProcess(
                listOf("dwarfdump", "--uuid", "shared.framework/shared"),
                projectPath.resolve("build/cocoapods/framework/").toFile()
            )

            val dsymResult = runProcess(
                listOf("dwarfdump", "--uuid", "shared.framework.dSYM"),
                projectPath.resolve("build/cocoapods/framework/").toFile()
            )

            assertContains(frameworkResult.output, "UUID:")
            assertContains(dsymResult.output, "UUID:")

            val frameworkUUID = frameworkResult.output.split(" ").getOrNull(1)
            val dsymUUID = dsymResult.output.split(" ").getOrNull(1)

            assertEquals(frameworkUUID, dsymUUID)
        }
    }

    @DisplayName("UTD after syncing framework")
    @GradleTest
    fun testImportUTDAfterSyncingFramework(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(
            nativeOptions = defaultBuildOptions.nativeOptions.copy(
                cocoapodsPlatform = "iphonesimulator",
                cocoapodsArchs = "x86_64",
                cocoapodsConfiguration = "Debug"
            )
        )

        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion, buildOptions = buildOptions) {
            buildGradleKts.addCocoapodsBlock(
                """
                    framework {
                        baseName = "kotlin-library"
                    }
                    name = "kotlin-library"
                    podfile = project.file("ios-app/Podfile")
                """.trimIndent()
            )

            buildWithCocoapodsWrapper(podImportTaskName) {
                assertTasksExecuted(dummyTaskName)
                assertTasksExecuted(podInstallTaskName)
            }

            buildWithCocoapodsWrapper(syncTaskName) {
                assertTasksExecuted(syncTaskName)
            }

            buildWithCocoapodsWrapper(podImportTaskName) {
                assertTasksExecuted(dummyTaskName)
                assertOutputContains("Skipping dummy-framework generation because a dynamic framework is already present")
                assertTasksUpToDate(podInstallTaskName)
            }
        }
    }

    @DisplayName("Changing framework type and checks UTD")
    @GradleTest
    fun testChangeFrameworkTypeUTD(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(
            nativeOptions = defaultBuildOptions.nativeOptions.copy(
                cocoapodsPlatform = "iphonesimulator",
                cocoapodsArchs = "x86_64",
                cocoapodsConfiguration = "Debug"
            )
        )

        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion, buildOptions = buildOptions) {
            buildGradleKts.addCocoapodsBlock(
                """
                    framework {
                        baseName = "kotlin-library"
                    }
                    name = "kotlin-library"
                    podfile = project.file("ios-app/Podfile")
                """.trimIndent()
            )

            buildWithCocoapodsWrapper(podImportTaskName) {
                assertTasksExecuted(dummyTaskName)
                assertTasksExecuted(podInstallTaskName)
            }

            buildWithCocoapodsWrapper(podImportTaskName) {
                assertTasksUpToDate(dummyTaskName)
                assertTasksUpToDate(podInstallTaskName)
            }

            buildWithCocoapodsWrapper(syncTaskName) {
                assertTasksExecuted(syncTaskName)
            }

            buildGradleKts.addFrameworkBlock("isStatic = true")
            buildWithCocoapodsWrapper(podImportTaskName) {
                assertTasksExecuted(dummyTaskName)
                assertOutputContains("Regenerating dummy-framework because present framework has different linkage")
                assertTasksExecuted(podInstallTaskName)
            }

            buildWithCocoapodsWrapper(podImportTaskName) {
                assertTasksUpToDate(dummyTaskName)
                assertTasksUpToDate(podInstallTaskName)
            }

        }
    }

    @DisplayName("UTD podspec")
    @GradleTest
    fun testUTDPodspec(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {

            buildWithCocoapodsWrapper(podspecTaskName)

            buildGradleKts.addCocoapodsBlock("license = \"new license name\"")
            buildWithCocoapodsWrapper(podspecTaskName) {
                assertTasksExecuted(podspecTaskName)
            }

            buildGradleKts.addCocoapodsBlock("license = \"new license name\"")
            buildWithCocoapodsWrapper(podspecTaskName) {
                assertTasksUpToDate(podspecTaskName)
            }
        }
    }

    @DisplayName("UTD with podspec deployment target")
    @GradleTest
    fun testUTDPodspecDeploymentTarget(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {

            buildWithCocoapodsWrapper(podspecTaskName)

            buildGradleKts.addCocoapodsBlock("ios.deploymentTarget = \"12.5\"")
            buildWithCocoapodsWrapper(podspecTaskName) {
                assertTasksExecuted(podspecTaskName)
            }

            buildWithCocoapodsWrapper(podspecTaskName) {
                assertTasksUpToDate(podspecTaskName)
            }
        }
    }

    @DisplayName("cinterops UTD after pod change")
    @GradleTest
    fun testCinteropsUTDAfterPodChange(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {

            val podDeclaration = """pod("Base64", version = "1.0.1")"""
            buildGradleKts.addCocoapodsBlock(podDeclaration)

            buildWithCocoapodsWrapper(podImportTaskName) {
                assertTasksExecuted(":cinteropBase64IOS")
            }

            buildWithCocoapodsWrapper(podImportTaskName) {
                assertTasksUpToDate(":cinteropBase64IOS")
            }

            buildGradleKts.replaceText(podDeclaration, """pod("Base64", version = "1.1.2")""")

            buildWithCocoapodsWrapper(podImportTaskName) {
                assertTasksExecuted(":cinteropBase64IOS")
            }
        }
    }

    @DisplayName("Installing pod without pod file")
    @GradleTest
    fun testPodInstallWithoutPodFile(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildWithCocoapodsWrapper(podInstallTaskName)
        }
    }

    @DisplayName("Pods with dependencies support")
    @GradleTest
    fun supportPodsWithDependencies(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addPod("AlamofireImage")

            buildWithCocoapodsWrapper(podImportTaskName) {
                podImportAsserts(buildGradleKts)
            }
        }
    }

    @DisplayName("Custom package name")
    @GradleTest
    fun testCustomPackageName(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {

            buildGradleKts.addPod("AFNetworking", "packageName = \"AFNetworking\"")
            val srcFileForChanging = projectPath.resolve("src/iosMain/kotlin/A.kt")
            srcFileForChanging.replaceText(
                "println(\"hi!\")", "println(AFNetworking.AFNetworkingReachabilityNotificationStatusItem)"
            )
            buildWithCocoapodsWrapper("assemble")
        }
    }

    @DisplayName("Cinterop extra opts")
    @GradleTest
    fun testCinteropExtraOpts(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addPod("AFNetworking", "extraOpts = listOf(\"-help\")")
            buildWithCocoapodsWrapper("cinteropAFNetworkingIOS") {
                assertOutputContains("Usage: cinterop options_list")
            }
        }
    }

    @DisplayName("Cocoapods with regular framework definition")
    @GradleTest
    fun testCocoapodsWithRegularFrameworkDefinition(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addKotlinBlock("iosX64(\"iOS\") {binaries.framework{}}")
            buildWithCocoapodsWrapper(podImportTaskName)
        }
    }

    @DisplayName("Checking sync framework")
    @GradleTest
    fun testSyncFramework(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {

            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsPlatform = "iphonesimulator",
                    cocoapodsArchs = "x86_64",
                    cocoapodsConfiguration = "Debug"
                )
            )
            build("syncFramework", buildOptions = buildOptions) {
                assertTasksExecuted(":linkPodDebugFrameworkIOS")
                assertFileInProjectExists("build/cocoapods/framework/cocoapods.framework/cocoapods")
            }
        }
    }

    @DisplayName("Sync framework with custom Xcode configuration")
    @GradleTest
    fun testSyncFrameworkCustomXcodeConfiguration(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addCocoapodsBlock("xcodeConfigurationToNativeBuildType[\"CUSTOM\"] = org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG\n")
            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsPlatform = "iphonesimulator",
                    cocoapodsArchs = "x86_64",
                    cocoapodsConfiguration = "CUSTOM"
                )
            )
            build("syncFramework", buildOptions = buildOptions) {
                assertTasksExecuted(":linkPodDebugFrameworkIOS")
                assertFileInProjectExists(("build/cocoapods/framework/cocoapods.framework/cocoapods"))
            }
        }
    }

    @DisplayName("Checking sync framework with invalid platform")
    @GradleTest
    fun testSyncFrameworkInvalidArch(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {

            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsPlatform = "iphoneos",
                    cocoapodsArchs = "x86_64",
                    cocoapodsConfiguration = "Debug"
                )
            )
            buildAndFail("syncFramework", buildOptions = buildOptions) {
                assertOutputContains("Architecture x86_64 is not supported for platform iphoneos")
            }
        }
    }

    @DisplayName("Checking sync framework with multiple platforms")
    @GradleTest
    fun testSyncFrameworkMultiplePlatforms(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {

            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsPlatform = "iphoneos iphonesimulator",
                    cocoapodsArchs = "arm64",
                    cocoapodsConfiguration = "Debug"
                )
            )
            buildAndFail("syncFramework", buildOptions = buildOptions) {
                assertOutputContains("kotlin.native.cocoapods.platform has to contain a single value only.")
            }
        }
    }

    @DisplayName("Sync framework multiple achitectures with custom name")
    @GradleTest
    fun testSyncFrameworkMultipleArchitecturesWithCustomName(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            val frameworkName = "customSdk"
            buildGradleKts.appendText(
                """
                    |
                    |kotlin {
                    |    iosSimulatorArm64()
                    |    cocoapods {
                    |       framework {
                    |           baseName = "$frameworkName"
                    |       }
                    |    }
                    |}
                """.trimMargin()
            )
            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsPlatform = "iphonesimulator",
                    cocoapodsArchs = "arm64 x86_64",
                    cocoapodsConfiguration = "Debug",
                    cocoapodsGenerateWrapper = true
                )
            )

            build("syncFramework", buildOptions = buildOptions) {
                // Check that an output framework is a dynamic framework
                val framework = projectPath.resolve("build/cocoapods/framework/$frameworkName.framework/$frameworkName")
                assertProcessRunResult(runProcess(listOf("file", framework.absolutePathString()), projectPath.toFile())) {
                    assertTrue(isSuccessful)
                    assertTrue(output.contains("universal binary with 2 architectures"))
                    assertTrue(output.contains("(for architecture x86_64)"))
                    assertTrue(output.contains("(for architecture arm64)"))
                }
            }
        }
    }

    @DisplayName("Xcode style errors when sync framework configuration failed")
    @GradleTest
    fun testSyncFrameworkUseXcodeStyleErrorsWhenConfigurationFailed(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.appendText(
                """
                kotlin {
                    sourceSets["commonMain"].dependencies {
                        implementation("com.example.unknown:dependency:0.0.1")
                    }       
                }
                """.trimIndent()
            )
            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsPlatform = "iphonesimulator",
                    cocoapodsArchs = "x86_64",
                    cocoapodsConfiguration = "Debug"
                )
            )
            buildAndFail("syncFramework", buildOptions = buildOptions) {
                assertOutputContains("error: Could not find com.example.unknown:dependency:0.0.1.")
            }
        }
    }

    @DisplayName("Xcode style errors when sync framework compilation failed")
    @GradleTest
    fun testSyncFrameworkUseXcodeStyleErrorsWhenCompilationFailed(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            projectPath.resolve("src/commonMain/kotlin/A.kt").appendText("this can't be compiled")
            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsPlatform = "iphonesimulator",
                    cocoapodsArchs = "x86_64",
                    cocoapodsConfiguration = "Debug"
                )
            )
            buildAndFail("syncFramework", buildOptions = buildOptions) {
                assertOutputContains("/native-cocoapods-template/src/commonMain/kotlin/A.kt:5:2: error: Syntax error: Expecting a top level declaration")
                assertOutputContains("error: Compilation finished with errors")
            }
        }
    }

    @DisplayName("Other tasks use gradle style errors when compilation failed")
    @GradleTest
    fun testOtherTasksUseGradleStyleErrorsWhenCompilationFailed(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            projectPath.resolve("src/commonMain/kotlin/A.kt").appendText("this can't be compiled")
            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsPlatform = "iphonesimulator",
                    cocoapodsArchs = "x86_64",
                    cocoapodsConfiguration = "Debug"
                )
            )
            buildAndFail("linkPodDebugFrameworkIOS", buildOptions = buildOptions) {
                assertOutputContains("e: file:///")
                assertOutputContains("/native-cocoapods-template/src/commonMain/kotlin/A.kt:5:2 Syntax error: Expecting a top level declaration")
                assertOutputDoesNotContain("error: Compilation finished with errors")
            }
        }
    }

    @DisplayName("Other tasks use Xcode style errors when compilation failed and `useXcodeMessageStyle` option enabled")
    @GradleTest
    fun testOtherTasksUseXcodeStyleErrorsWhenCompilationFailedAndOptionEnabled(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            projectPath.resolve("src/commonMain/kotlin/A.kt").appendText("this can't be compiled")
            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsPlatform = "iphonesimulator",
                    cocoapodsArchs = "x86_64",
                    cocoapodsConfiguration = "Debug",
                    useXcodeMessageStyle = true
                )
            )
            buildAndFail("linkPodDebugFrameworkIOS", buildOptions = buildOptions) {
                assertOutputContains("/native-cocoapods-template/src/commonMain/kotlin/A.kt:5:2: error: Syntax error: Expecting a top level declaration")
                assertOutputContains("error: Compilation finished with errors")
            }
        }
    }

    @DisplayName("Pod dependency in unit tests")
    @GradleTest
    fun testPodDependencyInUnitTests(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(cocoapodsTestsProjectName, gradleVersion) {
            buildWithCocoapodsWrapper(":iosX64Test")
        }
    }

    @DisplayName("Cinterop commonization off")
    @GradleTest
    fun testCinteropCommonizationOff(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(cocoapodsCommonizationProjectName, gradleVersion) {
            buildWithCocoapodsWrapper(":commonize", "-Pkotlin.mpp.enableCInteropCommonization=false") {
                assertTasksExecuted(":commonizeNativeDistribution")
                assertTasksAreNotInTaskGraph(
                    ":cinteropAFNetworkingIosArm64",
                    ":cinteropAFNetworkingIosX64",
                    ":commonizeCInterop",
                )
            }
        }
    }

    @DisplayName("Cinterop commonization on")
    @GradleTest
    fun testCinteropCommonizationOn(gradleVersion: GradleVersion) {
        testCinteropCommonizationExecutes(gradleVersion, buildArguments = arrayOf("-Pkotlin.mpp.enableCInteropCommonization=true"))
    }

    @DisplayName("Cinterop commonization unspecified")
    @GradleTest
    fun testCinteropCommonizationUnspecified(gradleVersion: GradleVersion) {
        testCinteropCommonizationExecutes(gradleVersion, buildArguments = emptyArray())
    }

    private fun testCinteropCommonizationExecutes(
        gradleVersion: GradleVersion,
        buildArguments: Array<String>,
    ) {
        nativeProjectWithCocoapodsAndIosAppPodFile(cocoapodsCommonizationProjectName, gradleVersion) {
            buildWithCocoapodsWrapper(":commonize", *buildArguments) {
                assertTasksExecuted(":commonizeNativeDistribution")
                assertTasksExecuted(":cinteropAFNetworkingIosArm64")
                assertTasksExecuted(":cinteropAFNetworkingIosX64")
                assertTasksExecuted(":commonizeCInterop")
            }
        }
    }

    @DisplayName("Checks pod publishing")
    @GradleTest
    fun testPodPublishing(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addKotlinBlock("iosX64(\"iOS\") {binaries.framework{}}")
            buildWithCocoapodsWrapper(":podPublishXCFramework") {
                assertTasksExecuted(":podPublishReleaseXCFramework")
                assertTasksExecuted(":podPublishDebugXCFramework")
                assertDirectoryInProjectExists("build/cocoapods/publish/release/cocoapods.xcframework")
                assertDirectoryInProjectExists("build/cocoapods/publish/debug/cocoapods.xcframework")
                assertFileInProjectExists("build/cocoapods/publish/release/cocoapods.podspec")
                assertFileInProjectExists("build/cocoapods/publish/debug/cocoapods.podspec")
                val actualPodspecContentWithoutBlankLines =
                    projectPath.resolve("build/cocoapods/publish/release/cocoapods.podspec").readText()
                        .lineSequence()
                        .filter { it.isNotBlank() }
                        .joinToString("\n")

                assertEquals(publishPodspecContent, actualPodspecContentWithoutBlankLines)
            }
        }
    }

    @DisplayName("Checks pod publishing with custom properties")
    @GradleTest
    fun testPodPublishingWithCustomProperties(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addCocoapodsBlock("name = \"CustomPod\"")
            buildGradleKts.addCocoapodsBlock("version = \"2.0\"")
            buildGradleKts.addCocoapodsBlock("publishDir = projectDir.resolve(\"CustomPublishDir\")")
            buildGradleKts.addCocoapodsBlock("license = \"'MIT'\"")
            buildGradleKts.addCocoapodsBlock("authors = \"{ 'Kotlin Dev' => 'kotlin.dev@jetbrains.com' }\"")
            buildGradleKts.addCocoapodsBlock("extraSpecAttributes[\"social_media_url\"] = \"'https://twitter.com/kotlin'\"")
            buildGradleKts.addCocoapodsBlock("extraSpecAttributes[\"vendored_frameworks\"] = \"'CustomFramework.xcframework'\"")
            buildGradleKts.addCocoapodsBlock("extraSpecAttributes[\"libraries\"] = \"'xml'\"")
            buildGradleKts.addPod(defaultPodName)

            buildWithCocoapodsWrapper(":podPublishXCFramework") {
                assertTasksExecuted(":podPublishReleaseXCFramework")
                assertTasksExecuted(":podPublishDebugXCFramework")
                assertDirectoryInProjectExists("CustomPublishDir/release/cocoapods.xcframework")
                assertDirectoryInProjectExists("CustomPublishDir/debug/cocoapods.xcframework")
                assertFileInProjectExists("CustomPublishDir/release/CustomPod.podspec")
                assertFileInProjectExists("CustomPublishDir/debug/CustomPod.podspec")
                val actualPodspecContentWithoutBlankLines = projectPath.resolve("CustomPublishDir/release/CustomPod.podspec").readText()
                    .lineSequence()
                    .filter { it.isNotBlank() }
                    .joinToString("\n")

                assertEquals(publishPodspecCustomContent, actualPodspecContentWithoutBlankLines)
            }
        }
    }

    @DisplayName("Checks pod install UTD")
    @GradleTest
    fun testPodInstallUpToDateCheck(gradleVersion: GradleVersion) {
        val subProjectName = "kotlin-library"
        val subprojectPodImportTask = ":$subProjectName$podImportTaskName"
        val subprojectPodspecTask = ":$subProjectName$podspecTaskName"
        val subprojectPodInstallTask = ":$subProjectName$podInstallTaskName"
        nativeProjectWithCocoapodsAndIosAppPodFile(cocoapodsSingleKtPod, gradleVersion) {
            buildGradleKts.addCocoapodsBlock("ios.deploymentTarget = \"14.0\"")
            buildWithCocoapodsWrapper(subprojectPodImportTask) {
                assertTasksExecuted(listOf(subprojectPodspecTask, subprojectPodInstallTask))
            }

            subProject(subProjectName).buildGradleKts.addPod(defaultPodName)
            buildWithCocoapodsWrapper(subprojectPodImportTask) {
                assertTasksExecuted(listOf(subprojectPodspecTask, subprojectPodInstallTask))
            }

            buildWithCocoapodsWrapper(subprojectPodImportTask) {
                assertTasksUpToDate(subprojectPodspecTask, subprojectPodInstallTask)
            }

            addPodToPodfile("ios-app", defaultPodName)
            buildWithCocoapodsWrapper(subprojectPodImportTask) {
                assertTasksUpToDate(subprojectPodspecTask)
                assertTasksExecuted(listOf(subprojectPodInstallTask))
            }
        }
    }

    @DisplayName("Cinterop Klibs provide linker opts to framework")
    @GradleTest
    fun testCinteropKlibsProvideLinkerOptsToFramework(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addPod("AFNetworking")
            buildWithCocoapodsWrapper("cinteropAFNetworkingIOS") {
                val cinteropKlib = projectPath.resolve("build/classes/kotlin/iOS/main/cinterop/cocoapods-cinterop-AFNetworking.klib")
                val manifestLines = ZipFile(cinteropKlib.toFile()).use { zip ->
                    zip.getInputStream(zip.getEntry("default/manifest")).bufferedReader().use { it.readLines() }
                }

                assertContains(manifestLines, "linkerOpts=-framework AFNetworking")
            }
        }
    }

    @DisplayName("Link only pods")
    @GradleTest
    fun testLinkOnlyPods(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addCocoapodsBlock(
                """
                    pod("AFNetworking") { linkOnly = true }
                    pod("SSZipArchive", linkOnly = true)
                    pod("SDWebImage/Core")
                """.trimIndent()
            )

            buildAndAssertAllTasks(
                notRegisteredTasks = listOf(":cinteropAFNetworkingIOS", ":cinteropSSZipArchiveIOS"),
                buildOptions = this.buildOptions.copy(
                    nativeOptions = this.buildOptions.nativeOptions.copy(
                        cocoapodsGenerateWrapper = true
                    )
                )
            )

            buildWithCocoapodsWrapper(":linkPodDebugFrameworkIOS") {
                assertTasksExecuted(":podBuildAFNetworkingIphonesimulator")
                assertTasksExecuted(":podBuildSDWebImageIphonesimulator")
                assertTasksExecuted(":podBuildSSZipArchiveIphonesimulator")

                assertTasksExecuted(":cinteropSDWebImageIOS")

                extractNativeTasksCommandLineArgumentsFromOutput(":linkPodDebugFrameworkIOS") {
                    assertCommandLineArgumentsContainSequentially("-linker-option", "-framework", "-linker-option", "AFNetworking")
                    assertCommandLineArgumentsContainSequentially("-linker-option", "-framework", "-linker-option", "SSZipArchive")
                }
            }
        }
    }

    @DisplayName("Usage link only with static framework produces message")
    @GradleTest
    fun testUsageLinkOnlyWithStaticFrameworkProducesMessage(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addCocoapodsBlock(
                """
                    framework {
                        isStatic = true
                    }
        
                    pod("AFNetworking") { linkOnly = true }
                """.trimIndent()
            )
            buildWithCocoapodsWrapper(":linkPodDebugFrameworkIOS") {
                assertHasDiagnostic(CocoapodsPluginDiagnostics.LinkOnlyUsedWithStaticFramework)
            }
        }
    }

    @DisplayName("Add pod-dependencies together with noPodspec")
    @GradleTest
    fun testPodDependenciesWithNoPodspec(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addCocoapodsBlock(
                """
                    noPodspec()
        
                    pod("Base64", version = "1.1.2")
                """.trimIndent()
            )
            buildWithCocoapodsWrapper(":linkPodDebugFrameworkIOS") {
                assertFileInProjectNotExists("cocoapods.podspec")
            }
        }
    }

    @DisplayName("Hierarchy of dependant pods compiles successfully")
    @GradleTest
    fun testHierarchyOfDependantPodsCompilesSuccessfully(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(cocoapodsDependantPodsProjectName, gradleVersion) {
            buildWithCocoapodsWrapper(":compileKotlinIosX64")
        }
    }

    @DisplayName("Error reported when trying to depend on non-declared pod")
    @GradleTest
    fun testErrorReportedWhenTryingToDependOnNonDeclaredPod(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(cocoapodsDependantPodsProjectName, gradleVersion) {
            buildGradleKts.addCocoapodsBlock(
                """
                    pod("Foo") { useInteropBindingFrom("JBNonExistent") }
                """.trimIndent()
            )

            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsGenerateWrapper = true
                )
            )

            build(":help", buildOptions = buildOptions) {
                assertOutputContains("Couldn't find declaration of pod 'JBNonExistent' (interop-binding dependency of pod 'Foo')")
            }
        }
    }

    @DisplayName("Error reported when dependant pods are in the wrong order")
    @GradleTest
    fun testErrorReportedWhenDependantPodsAreInTheWrongOrder(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(cocoapodsDependantPodsProjectName, gradleVersion) {
            buildGradleKts.addCocoapodsBlock(
                """
                    pod("Foo") { useInteropBindingFrom("Bar") }
                    pod("Bar")
                """.trimIndent()
            )

            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsGenerateWrapper = true
                )
            )

            build(":help", buildOptions = buildOptions) {
                assertHasDiagnostic(CocoapodsPluginDiagnostics.InteropBindingUnknownDependency)
            }
        }
    }

    @DisplayName("Error reported when pod depends on itself")
    @GradleTest
    fun testErrorReportedWhenPodDependsOnItself(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(cocoapodsDependantPodsProjectName, gradleVersion) {
            buildGradleKts.addCocoapodsBlock(
                """
                    pod("Foo") { useInteropBindingFrom("Foo") }
                """.trimIndent()
            )

            val buildOptions = this.buildOptions.copy(
                nativeOptions = this.buildOptions.nativeOptions.copy(
                    cocoapodsGenerateWrapper = true
                )
            )

            build(":help", buildOptions = buildOptions) {
                assertHasDiagnostic(CocoapodsPluginDiagnostics.InteropBindingSelfDependency)
            }
        }
    }

    @DisplayName("Configuration cache works in a complex scenario")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_6)
    @GradleTest
    fun testConfigurationCacheWorksInAComplexScenario(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(
            nativeOptions = defaultBuildOptions.nativeOptions.copy(
                cocoapodsGenerateWrapper = true,
                cocoapodsPlatform = "iphonesimulator",
                cocoapodsArchs = "x86_64",
                cocoapodsConfiguration = "Debug"
            ),
            configurationCache = true
        )
        nativeProjectWithCocoapodsAndIosAppPodFile(
            gradleVersion = gradleVersion,
            buildOptions = buildOptions
        ) {
            buildGradleKts.addCocoapodsBlock("""pod("Base64", version = "1.1.2")""")

            val tasks = arrayOf(
                ":podspec",
                ":podImport",
                ":podPublishDebugXCFramework",
                ":podPublishReleaseXCFramework",
                ":syncFramework",
            )

            val executableTasks = listOf(
                ":podspec",
                ":podPublishDebugXCFramework",
                ":podPublishReleaseXCFramework",
                ":linkPodDebugFrameworkIOS",
            )

            build(*tasks) {
                assertTasksExecuted(executableTasks)

                assertOutputContains("Calculating task graph as no configuration cache is available for tasks")

                assertOutputContains("Configuration cache entry stored.")
            }

            build("clean")

            build(*tasks) {
                assertOutputContains("Reusing configuration cache.")
            }

            build(*tasks) {
                assertTasksUpToDate(executableTasks)
            }
        }
    }

    private val maybeCocoaPodsIsNotInstalledError = "Possible reason: CocoaPods is not installed"
    private val maybePodfileIsIncorrectError = "Please, check that podfile contains following lines in header"
    private val missingPodExecutableInPath = "CocoaPods executable not found in your PATH"

    @DisplayName("Pod install emits correct error when pod binary is not present in PATH")
    @GradleTest
    fun testPodInstallErrorWithoutCocoaPodsInPATH(gradleVersion: GradleVersion) {
        val pathWithoutCocoapods = "/bin:/usr/bin"
        nativeProjectWithCocoapodsAndIosAppPodFile(
            gradleVersion = gradleVersion,
            environmentVariables = EnvironmentalVariables(
                mapOf("PATH" to pathWithoutCocoapods)
            )
        ) {
            buildGradleKts.addCocoapodsBlock(
                """
                    podfile = project.file("ios-app/Podfile")
                """.trimIndent()
            )

            buildAndFailWithCocoapodsWrapper(
                podInstallTaskName,
            ) {
                assertOutputDoesNotContain(maybePodfileIsIncorrectError)
                assertOutputDoesNotContain(maybeCocoaPodsIsNotInstalledError)
                assertOutputContains(missingPodExecutableInPath)
            }
        }
    }

    @DisplayName("Pod install emits other errors when pod install runs, but fails later")
    @GradleTest
    fun testOtherPodInstallErrors(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(
            gradleVersion = gradleVersion
        ) {
            buildGradleKts.addCocoapodsBlock(
                """
                    podfile = project.file("ios-app/Podfile")
                """.trimIndent()
            )

            projectPath.resolve("ios-app/Podfile").append(
                """
                    raise "Dead"
                """.trimIndent()
            )

            buildAndFailWithCocoapodsWrapper(
                podInstallTaskName,
            ) {
                assertOutputContains(maybePodfileIsIncorrectError)
                assertOutputDoesNotContain(maybeCocoaPodsIsNotInstalledError)
            }
        }
    }

    @DisplayName("Installing pod with custom defined pod executable in the local.properties")
    @GradleTest
    fun testPodInstallWithCustomExecutablePath(gradleVersion: GradleVersion) {
        val podPathRun = runProcess(listOf("which", "pod"), Path("/").toFile())
        val pathWithoutCocoapods = "/bin:/usr/bin"
        nativeProjectWithCocoapodsAndIosAppPodFile(
            gradleVersion = gradleVersion,
            environmentVariables = EnvironmentalVariables(
                mapOf("PATH" to pathWithoutCocoapods)
            )
        ) {

            val podPath = podPathRun.output.removingTrailingNewline()

            assertTrue {
                podPath.isNotBlank()
            }
            assertTrue {
                podPathRun.exitCode != 1
            }

            buildGradleKts.addCocoapodsBlock(
                """
                    framework {
                        baseName = "kotlin-library"
                    }
                    name = "kotlin-library"
                    podfile = project.file("ios-app/Podfile")
                """.trimIndent()
            )

            buildAndFailWithCocoapodsWrapper(podInstallTaskName) {
                assertOutputContains(missingPodExecutableInPath)
            }

            projectPath.resolve("local.properties")
                .also { if (!it.exists()) it.createFile() }
                .apply {
                    append("\n")
                    appendText(
                        """
                            kotlin.apple.cocoapods.bin=${podPath}
                        """.trimIndent()
                    )
                }

            buildWithCocoapodsWrapper(podInstallTaskName) {
                assertTasksExecuted(podInstallTaskName)
            }
        }
    }

    private fun TestProject.buildAndFailWithCocoapodsWrapper(
        vararg buildArguments: String,
        assertions: BuildResult.() -> Unit = {},
    ) = buildWithCocoapodsWrapperUsing { buildOptions ->
        buildAndFail(
            *buildArguments,
            buildOptions = buildOptions,
            assertions = assertions,
        )
    }

    private fun TestProject.buildWithCocoapodsWrapper(
        vararg buildArguments: String,
        assertions: BuildResult.() -> Unit = {},
    ) = buildWithCocoapodsWrapperUsing { buildOptions ->
        build(
            *buildArguments,
            buildOptions = buildOptions,
            assertions = assertions,
        )
    }

    private fun TestProject.buildWithCocoapodsWrapperUsing(
        builder: TestProject.(BuildOptions) -> Unit,
    ) {
        val buildOptions = this.buildOptions.copy(
            nativeOptions = this.buildOptions.nativeOptions.copy(
                cocoapodsGenerateWrapper = true
            )
        )
        builder(buildOptions)
    }

    private fun TestProject.addPodToPodfile(iosAppLocation: String, pod: String) {
        projectPath
            .resolve(iosAppLocation)
            .resolve("Podfile")
            .replaceText(podfileImportPodPlaceholder, "pod '$pod'")
    }

    private val publishPodspecContent =
        """
            Pod::Spec.new do |spec|
                spec.name                     = 'cocoapods'
                spec.version                  = '1.0'
                spec.homepage                 = 'https://github.com/JetBrains/kotlin'
                spec.source                   = { :http=> ''}
                spec.authors                  = ''
                spec.license                  = ''
                spec.summary                  = 'CocoaPods test library'
                spec.vendored_frameworks      = 'cocoapods.xcframework'
                spec.libraries                = 'c++'
                spec.ios.deployment_target    = '13.5'
            end
        """.trimIndent()

    private val publishPodspecCustomContent =
        """
            Pod::Spec.new do |spec|
                spec.name                     = 'CustomPod'
                spec.version                  = '2.0'
                spec.homepage                 = 'https://github.com/JetBrains/kotlin'
                spec.source                   = { :http=> ''}
                spec.authors                  = { 'Kotlin Dev' => 'kotlin.dev@jetbrains.com' }
                spec.license                  = 'MIT'
                spec.summary                  = 'CocoaPods test library'
                spec.ios.deployment_target    = '13.5'
                spec.dependency 'AFNetworking'
                spec.social_media_url = 'https://twitter.com/kotlin'
                spec.vendored_frameworks = 'CustomFramework.xcframework'
                spec.libraries = 'xml'
            end
        """.trimIndent()
}
