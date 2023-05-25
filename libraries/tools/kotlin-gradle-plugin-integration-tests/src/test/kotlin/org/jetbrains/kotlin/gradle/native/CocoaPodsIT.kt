/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.DUMMY_FRAMEWORK_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_BUILD_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_GEN_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_IMPORT_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_INSTALL_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_SETUP_BUILD_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_SPEC_TASK_NAME
import org.jetbrains.kotlin.gradle.testbase.ImportMode
import org.jetbrains.kotlin.gradle.testbase.cocoaPodsEnvironmentVariables
import org.jetbrains.kotlin.gradle.testbase.ensureCocoapodsInstalled
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class CocoaPodsIT : BaseGradleIT() {

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    // We use Kotlin DSL. Earlier Gradle versions fail at accessors codegen.
    private val gradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT

    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(customEnvironmentVariables = cocoaPodsEnvironmentVariables())

    private val podfileImportDirectivePlaceholder = "<import_mode_directive>"
    private val podfileImportPodPlaceholder = "#import_pod_directive"

    private val cocoapodsSingleKtPod = "native-cocoapods-single"
    private val cocoapodsMultipleKtPods = "native-cocoapods-multiple"
    private val templateProjectName = "native-cocoapods-template"
    private val cocoapodsTestsProjectName = "native-cocoapods-tests"
    private val cocoapodsCommonizationProjectName = "native-cocoapods-commonization"

    private val dummyTaskName = ":$DUMMY_FRAMEWORK_TASK_NAME"
    private val podspecTaskName = ":$POD_SPEC_TASK_NAME"
    private val podGenTaskName = ":$POD_GEN_TASK_NAME"
    private val podImportTaskName = ":$POD_IMPORT_TASK_NAME"
    private val podInstallTaskName = ":$POD_INSTALL_TASK_NAME"

    private val defaultPodRepo = "https://github.com/AFNetworking/AFNetworking"
    private val defaultPodName = "AFNetworking"
    private val defaultFamily = "ios"
    private val defaultPodGenTaskName = podGenFullTaskName()
    private val defaultPodInstallSyntheticTaskName = ":podInstallSyntheticIos"

    private fun podGenFullTaskName(familyName: String = defaultFamily) =
        podGenTaskName + familyName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    private lateinit var hooks: CustomHooks
    private lateinit var project: BaseGradleIT.Project

    @Before
    fun configure() {
        hooks = CustomHooks()
        project = getProjectByName(templateProjectName).apply {
            preparePodfile("ios-app", ImportMode.FRAMEWORKS)
        }
    }

    @Test
    fun testPodImportSingle() {
        val project = getProjectByName(cocoapodsSingleKtPod)

        project.preparePodfile("ios-app", ImportMode.FRAMEWORKS)

        project.testImportWithAsserts()

        hooks.rewriteHooks {
            podImportAsserts("kotlin-library")
        }
        project.testSynthetic(":kotlin-library:podImport")
    }

    @Test
    fun testPodImportMultiple() {
        val project = getProjectByName(cocoapodsMultipleKtPods)

        project.preparePodfile("ios-app", ImportMode.FRAMEWORKS)

        project.testImportWithAsserts()

        hooks.rewriteHooks {
            podImportAsserts("kotlin-library")
        }
        project.testSynthetic(":kotlin-library:podImport")

        hooks.rewriteHooks {
            podImportAsserts("second-library")
        }
        project.testSynthetic(":second-library:podImport")
    }

    @Test
    fun testSpecReposImport() {
        val podName = "example"
        val podRepo = "https://github.com/alozhkin/spec_repo"
        with(project.gradleBuildScript()) {
            addPod(podName)
            addSpecRepo(podRepo)
        }
        project.testImportWithAsserts(listOf(podRepo))
    }

    @Test
    fun testSyntheticProjectPodfileGeneration() {
        val gradleProject = transformProjectWithPluginsDsl(cocoapodsSingleKtPod, gradleVersion)
        gradleProject.gradleBuildScript().appendToCocoapodsBlock(
            """
            ios.deploymentTarget = "14.1"
            pod("SSZipArchive")
            pod("AFNetworking", "~> 4.0.1")
            pod("Alamofire") {
                source = git("https://github.com/Alamofire/Alamofire.git") {
                    tag = "5.6.1"
                }
            }
        """.trimIndent()
        )
        gradleProject.build("podInstallSyntheticIos", "-Pkotlin.native.cocoapods.generate.wrapper=true") {
            assertSuccessful()
            assertTasksExecuted(":podGenIos")

            val podfileText = gradleProject.projectDir.resolve("build/cocoapods/synthetic/ios/Podfile").readText().trim()
            assertTrue(podfileText.contains("platform :ios, '14.1'"))
            assertTrue(podfileText.contains("pod 'SSZipArchive'"))
            assertTrue(podfileText.contains("pod 'AFNetworking', '~> 4.0.1'"))
            assertTrue(podfileText.contains("pod 'Alamofire', :git => 'https://github.com/Alamofire/Alamofire.git', :tag => '5.6.1'"))
            assertTrue(podfileText.contains("config.build_settings['EXPANDED_CODE_SIGN_IDENTITY'] = \"\""))
            assertTrue(podfileText.contains("config.build_settings['CODE_SIGNING_REQUIRED'] = \"NO\""))
            assertTrue(podfileText.contains("config.build_settings['CODE_SIGNING_ALLOWED'] = \"NO\""))
        }
    }

    @Test
    fun testSyntheticProjectPodfilePostprocessing() {
        project.gradleBuildScript().apply {
            appendToCocoapodsBlock("""pod("ChatSDK", version = "5.2.1")""")

            appendText(
                """
                
                tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.PodGenTask>().configureEach {
                    doLast {
                        podfile.get().appendText("ENV['SWIFT_VERSION'] = '5'")
                    }
                }
            """.trimIndent()
            )
        }

        project.build("podInstallSyntheticIos", "-Pkotlin.native.cocoapods.generate.wrapper=true") {
            assertSuccessful()
            assertFileContains(path = "build/cocoapods/synthetic/ios/Podfile", "ENV['SWIFT_VERSION'] = '5'")
        }
    }

    @Test
    fun warnIfDeprecatedPodspecPathIsUsed() {
        project = getProjectByName(cocoapodsSingleKtPod)
        hooks.addHook {
            assertContains(
                listOf("Deprecated DSL found on ${project.projectDir.absolutePath}", "kotlin-library", "build.gradle.kts")
                    .joinToString(separator = File.separator)
            )
        }
        project.test(":kotlin-library:tasks")
    }

    @Test
    fun errorIfVersionIsNotSpecified() {
        with(project.gradleBuildScript()) {
            useLines { lines ->
                lines.filter { line -> "version = \"1.0\"" !in line }.joinToString(separator = "\n")
            }.also { writeText(it) }
        }
        hooks.addHook {
            assertContains("Cocoapods Integration requires pod version to be specified.")
        }

        project.build(POD_IMPORT_TASK_NAME, "-Pkotlin.native.cocoapods.generate.wrapper=true") {
            assertFailed()
            hooks.trigger(this)
        }
    }

    // up-to-date tests

    @Test
    fun testDummyUTD() {
        hooks.addHook {
            assertTasksExecuted(dummyTaskName)
        }
        project.testWithWrapper(dummyTaskName)

        hooks.rewriteHooks {
            assertTasksUpToDate(dummyTaskName)
        }
        project.testWithWrapper(dummyTaskName)
    }

    @Test
    fun testImportUTDAfterLinkingFramework() {
        val linkTaskName = ":linkPodDebugFrameworkIOS"
        project.gradleBuildScript().appendToCocoapodsBlock(
            """
            framework {
                baseName = "kotlin-library"
            }
            name = "kotlin-library"
            podfile = project.file("ios-app/Podfile")
        """.trimIndent()
        )


        hooks.addHook {
            assertTasksExecuted(dummyTaskName)
            assertTasksExecuted(podInstallTaskName)
        }
        project.testImport()

        hooks.rewriteHooks {
            assertTasksExecuted(linkTaskName)
        }
        project.testWithWrapper(linkTaskName)

        hooks.rewriteHooks {
            assertTasksUpToDate(dummyTaskName)
            assertTasksUpToDate(podInstallTaskName)
        }
        project.testImport()
    }

    @Test
    fun testChangeFrameworkTypeUTD() {
        project.gradleBuildScript().appendToCocoapodsBlock(
            """
            framework {
                baseName = "kotlin-library"
            }
            name = "kotlin-library"
            podfile = project.file("ios-app/Podfile")
        """.trimIndent()
        )

        hooks.addHook {
            assertTasksExecuted(dummyTaskName)
            assertTasksExecuted(podInstallTaskName)
        }
        project.testImport()

        hooks.rewriteHooks {
            assertTasksUpToDate(dummyTaskName)
            assertTasksUpToDate(podInstallTaskName)
        }
        project.testImport()

        project.gradleBuildScript().appendToFrameworkBlock("isStatic = true")

        hooks.rewriteHooks {
            assertTasksExecuted(dummyTaskName)
            assertTasksExecuted(podInstallTaskName)
        }
        project.testImport()

        hooks.rewriteHooks {
            assertTasksUpToDate(dummyTaskName)
            assertTasksUpToDate(podInstallTaskName)
        }
        project.testImport()
    }


    @Test
    fun testSpecReposUTD() {
        with(project.gradleBuildScript()) {
            addPod("AFNetworking")
        }
        hooks.addHook {
            assertTasksExecuted(defaultPodGenTaskName)
        }
        project.testSynthetic(defaultPodGenTaskName)
        with(project.gradleBuildScript()) {
            addSpecRepo("https://github.com/alozhkin/spec_repo_example.git")
        }
        project.testSynthetic(defaultPodGenTaskName)
        hooks.rewriteHooks {
            assertTasksUpToDate(defaultPodGenTaskName)
        }
        project.testSynthetic(defaultPodGenTaskName)
    }

    @Test
    fun testPodInstallInvalidatesUTD() {
        with(project.gradleBuildScript()) {
            addPod("AFNetworking")
        }

        hooks.addHook {
            assertTasksExecuted(defaultPodInstallSyntheticTaskName)
            assertTrue { fileInWorkingDir("build/cocoapods/synthetic/ios/Pods/AFNetworking").deleteRecursively() }
        }
        project.testSynthetic(defaultPodInstallSyntheticTaskName)

        hooks.rewriteHooks {
            assertTasksExecuted(defaultPodInstallSyntheticTaskName)
        }
        project.testSynthetic(defaultPodInstallSyntheticTaskName)
    }

    @Test
    fun testImportSubspecs() {
        with(project.gradleBuildScript()) {
            addPod("SDWebImage/Core")
            addPod("SDWebImage/MapKit")
        }
        project.testImport(listOf(defaultPodRepo))
    }

    @Test
    fun testUTDPodspec() {
        project.testWithWrapper(podspecTaskName)
        hooks.addHook {
            assertTasksExecuted(podspecTaskName)
        }
        with(project.gradleBuildScript()) {
            appendToCocoapodsBlock("license = \"new license name\"")
        }
        project.testWithWrapper(podspecTaskName)
        with(project.gradleBuildScript()) {
            appendToCocoapodsBlock("license = \"new license name\"")
        }
        hooks.rewriteHooks {
            assertTasksUpToDate(podspecTaskName)
        }
        project.testWithWrapper(podspecTaskName)
    }

    @Test
    fun testUTDPodspecDeploymentTarget() {
        project.testWithWrapper(podspecTaskName)
        hooks.addHook {
            assertTasksExecuted(podspecTaskName)
        }
        with(project.gradleBuildScript()) {
            appendToCocoapodsBlock("ios.deploymentTarget = \"12.5\"")
        }
        project.testWithWrapper(podspecTaskName)
        hooks.rewriteHooks {
            assertTasksUpToDate(podspecTaskName)
        }
        project.testWithWrapper(podspecTaskName)
    }


    @Test
    fun testPodInstallWithoutPodFile() {
        project.testSynthetic(podInstallTaskName)
    }


    // other tests

    @Test
    fun supportPodsWithDependencies() {
        with(project.gradleBuildScript()) {
            addPod("AlamofireImage")
        }
        project.testImportWithAsserts()
    }

    @Test
    fun testCustomPackageName() {
        with(project.gradleBuildScript()) {
            addPod("AFNetworking", "packageName = \"AFNetworking\"")
        }
        with(project) {
            File(projectDir, "src/iosMain/kotlin/A.kt").modify {
                it.replace(
                    "fun foo() {", """
                import AFNetworking
                fun foo() {
            """.trimIndent()
                )
                it.replace("println(\"hi!\")", "println(AFNetworking.AFNetworkingReachabilityNotificationStatusItem)")
            }

            testWithWrapper("assemble")
        }
    }

    @Test
    fun testCinteropExtraOpts() {
        with(project) {
            gradleBuildScript().addPod("AFNetworking", "extraOpts = listOf(\"-help\")")
            hooks.addHook {
                assertContains("Usage: cinterop options_list")
            }
            testWithWrapper("cinteropAFNetworkingIOS")
        }
    }

    @Test
    fun testUseLibrariesMode() {
        with(project) {
            gradleBuildScript().appendToCocoapodsBlock("useLibraries()")
            gradleBuildScript().addPod("AFNetworking", configuration = "headers = \"AFNetworking/AFNetworking.h\"")
            testImport()
        }
    }


    @Test
    fun testUseLibrariesModeWarnWhenPodIsAddedWithoutHeadersSpecified() {
        with(project) {
            gradleBuildScript().appendToCocoapodsBlock("useLibraries()")
            gradleBuildScript().addPod("AFNetworking")

            hooks.addHook {
                assertContains("w: Pod 'AFNetworking' should have 'headers' property specified when using 'useLibraries()'")
            }

            testImport()
        }
    }


    @Test
    fun testCocoapodsWithRegularFrameworkDefinition() {
        with(project) {
            gradleBuildScript().appendToKotlinBlock("iosX64(\"iOS\") {binaries.framework{}}")
            testImport()
        }
    }

    @Test
    fun testSyncFramework() {
        with(project) {
            hooks.addHook {
                assertTasksExecuted(":linkPodDebugFrameworkIOS")
                assertTrue(fileInWorkingDir("build/cocoapods/framework/cocoapods.framework/cocoapods").exists())
            }
            test(
                "syncFramework",
                "-Pkotlin.native.cocoapods.platform=iphonesimulator",
                "-Pkotlin.native.cocoapods.archs=x86_64",
                "-Pkotlin.native.cocoapods.configuration=Debug"
            )
        }
    }

    @Test
    fun testSyncFrameworkCustomXcodeConfiguration() {
        with(project) {
            gradleBuildScript().appendToCocoapodsBlock("xcodeConfigurationToNativeBuildType[\"CUSTOM\"] = org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG\n")
            hooks.addHook {
                assertTasksExecuted(":linkPodDebugFrameworkIOS")
                assertTrue(fileInWorkingDir("build/cocoapods/framework/cocoapods.framework/cocoapods").exists())
            }
            test(
                "syncFramework",
                "-Pkotlin.native.cocoapods.platform=iphonesimulator",
                "-Pkotlin.native.cocoapods.archs=x86_64",
                "-Pkotlin.native.cocoapods.configuration=CUSTOM"
            )
        }
    }

    @Test
    fun testSyncFrameworkInvalidArch() {
        with(project) {
            build(
                "syncFramework",
                "-Pkotlin.native.cocoapods.platform=iphoneos",
                "-Pkotlin.native.cocoapods.archs=x86_64",
                "-Pkotlin.native.cocoapods.configuration=Debug"
            ) {
                assertFailed()
                assertContains("Architecture x86_64 is not supported for platform iphoneos")
            }
        }
    }

    @Test
    fun testSyncFrameworkMultiplePlatforms() {
        with(project) {
            build(
                "syncFramework",
                "-Pkotlin.native.cocoapods.platform=iphoneos iphonesimulator",
                "-Pkotlin.native.cocoapods.archs=arm64",
                "-Pkotlin.native.cocoapods.configuration=Debug"
            ) {
                assertFailed()
                assertContains("kotlin.native.cocoapods.platform has to contain a single value only.")
            }
        }
    }

    @Test
    fun testSyncFrameworkMultipleArchitecturesWithCustomName() {
        with(project) {
            val frameworkName = "customSdk"
            gradleBuildScript().appendText(
                """
                    |
                    |kotlin {
                    |    iosArm64()
                    |    iosArm32()
                    |    cocoapods {
                    |       framework {
                    |           baseName = "$frameworkName"
                    |       }
                    |    }
                    |}
                """.trimMargin()
            )
            hooks.addHook {
                // Check that an output framework is a dynamic framework
                val framework = fileInWorkingDir("build/cocoapods/framework/$frameworkName.framework/$frameworkName")
                with(runProcess(listOf("file", framework.absolutePath), projectDir)) {
                    assertTrue(isSuccessful)
                    assertTrue(output.contains("universal binary with 2 architectures"))
                    assertTrue(output.contains("(for architecture armv7)"))
                    assertTrue(output.contains("(for architecture arm64)"))
                }
            }

            test(
                "syncFramework",
                "-Pkotlin.native.cocoapods.platform=iphoneos",
                "-Pkotlin.native.cocoapods.archs=arm64 armv7",
                "-Pkotlin.native.cocoapods.configuration=Debug",
                "-Pkotlin.native.cocoapods.generate.wrapper=true"
            )
        }
    }


    @Test
    fun testSyncFrameworkUseXcodeStyleErrorsWhenConfigurationFailed() {
        with(project) {
            gradleBuildScript().appendText(
                """
                kotlin {
                    sourceSets["commonMain"].dependencies {
                        implementation("com.example.unknown:dependency:0.0.1")
                    }       
                }
                """.trimIndent()
            )

            build(
                "syncFramework",
                "-Pkotlin.native.cocoapods.platform=iphonesimulator",
                "-Pkotlin.native.cocoapods.archs=x86_64",
                "-Pkotlin.native.cocoapods.configuration=Debug"
            ) {
                assertFailed()
                assertContains("error: Could not find com.example.unknown:dependency:0.0.1.")
            }
        }
    }

    @Test
    fun testSyncFrameworkUseXcodeStyleErrorsWhenCompilationFailed() {
        with(project) {
            projectDir.resolve("src/commonMain/kotlin/A.kt").appendText("this can't be compiled")

            build(
                "syncFramework",
                "-Pkotlin.native.cocoapods.platform=iphonesimulator",
                "-Pkotlin.native.cocoapods.archs=x86_64",
                "-Pkotlin.native.cocoapods.configuration=Debug",
            ) {
                assertFailed()
                assertContains("/native-cocoapods-template/src/commonMain/kotlin/A.kt:5:2: error: Expecting a top level declaration")
                assertContains("error: Compilation finished with errors")
            }
        }
    }

    @Test
    fun testOtherTasksUseGradleStyleErrorsWhenCompilationFailed() {
        with(project) {
            projectDir.resolve("src/commonMain/kotlin/A.kt").appendText("this can't be compiled")

            build("linkPodDebugFrameworkIOS") {
                assertFailed()
                assertContains("e: file:///")
                assertContains("/native-cocoapods-template/src/commonMain/kotlin/A.kt:5:2 Expecting a top level declaration")
                assertNotContains("error: Compilation finished with errors")
            }
        }
    }

    @Test
    fun testOtherTasksUseXcodeStyleErrorsWhenCompilationFailedAndOptionEnabled() {
        with(project) {
            projectDir.resolve("src/commonMain/kotlin/A.kt").appendText("this can't be compiled")

            build("linkPodDebugFrameworkIOS", "-Pkotlin.native.useXcodeMessageStyle=true") {
                assertFailed()
                assertContains("/native-cocoapods-template/src/commonMain/kotlin/A.kt:5:2: error: Expecting a top level declaration")
                assertContains("error: Compilation finished with errors")
            }
        }
    }

    @Test
    fun testPodDependencyInUnitTests() {
        getProjectByName(cocoapodsTestsProjectName).testWithWrapper(":iosX64Test")
    }


    @Test
    fun testCinteropCommonizationOff() {
        project = getProjectByName(cocoapodsCommonizationProjectName)
        hooks.addHook {
            assertTasksExecuted(":commonizeNativeDistribution")
            assertTasksNotExecuted(":cinteropAFNetworkingIosArm64")
            assertTasksNotExecuted(":cinteropAFNetworkingIosX64")
            assertTasksNotExecuted(":commonizeCInterop")
        }
        project.testWithWrapper(":commonize")
    }

    @Test
    fun testCinteropCommonizationOn() {
        project = getProjectByName(cocoapodsCommonizationProjectName)
        project.gradleProperties().appendLine("kotlin.mpp.enableCInteropCommonization=true")
        hooks.addHook {
            assertTasksExecuted(":commonizeNativeDistribution")
            assertTasksExecuted(":cinteropAFNetworkingIosArm64")
            assertTasksExecuted(":cinteropAFNetworkingIosX64")
            assertTasksExecuted(":commonizeCInterop")
        }
        project.testWithWrapper(":compileIosMainKotlinMetadata")
    }

    @Test
    fun testPodPublishing() {
        //test that manually created frameworks are not included into cocoapods xcframework
        project.gradleBuildScript().appendToKotlinBlock("iosX64(\"iOS\") {binaries.framework{}}")
        project.build(
            ":podPublishXCFramework",
            "-Pkotlin.native.cocoapods.generate.wrapper=true",
        ) {
            assertSuccessful()

            assertTasksExecuted(":podPublishReleaseXCFramework")
            assertTasksExecuted(":podPublishDebugXCFramework")
            assertFileExists("build/cocoapods/publish/release/cocoapods.xcframework")
            assertFileExists("build/cocoapods/publish/debug/cocoapods.xcframework")
            assertFileExists("build/cocoapods/publish/release/cocoapods.podspec")
            assertFileExists("build/cocoapods/publish/debug/cocoapods.podspec")
            val actualPodspecContentWithoutBlankLines = fileInWorkingDir("build/cocoapods/publish/release/cocoapods.podspec").readText()
                .lineSequence()
                .filter { it.isNotBlank() }
                .joinToString("\n")

            assertEquals(publishPodspecContent, actualPodspecContentWithoutBlankLines)
        }
    }


    @Test
    fun testPodPublishingWithCustomProperties() {

        with(project.gradleBuildScript()) {
            appendToCocoapodsBlock("name = \"CustomPod\"")
            appendToCocoapodsBlock("version = \"2.0\"")
            appendToCocoapodsBlock("publishDir = projectDir.resolve(\"CustomPublishDir\")")
            appendToCocoapodsBlock("license = \"'MIT'\"")
            appendToCocoapodsBlock("authors = \"{ 'Kotlin Dev' => 'kotlin.dev@jetbrains.com' }\"")
            appendToCocoapodsBlock("extraSpecAttributes[\"social_media_url\"] = \"'https://twitter.com/kotlin'\"")
            appendToCocoapodsBlock("extraSpecAttributes[\"vendored_frameworks\"] = \"'CustomFramework.xcframework'\"")
            appendToCocoapodsBlock("extraSpecAttributes[\"libraries\"] = \"'xml'\"")
            addPod(defaultPodName)
        }

        hooks.addHook {
            assertTasksExecuted(":podPublishReleaseXCFramework")
            assertTasksExecuted(":podPublishDebugXCFramework")
            assertFileExists("CustomPublishDir/release/cocoapods.xcframework")
            assertFileExists("CustomPublishDir/debug/cocoapods.xcframework")
            assertFileExists("CustomPublishDir/release/CustomPod.podspec")
            assertFileExists("CustomPublishDir/debug/CustomPod.podspec")
            val actualPodspecContentWithoutBlankLines = fileInWorkingDir("CustomPublishDir/release/CustomPod.podspec").readText()
                .lineSequence()
                .filter { it.isNotBlank() }
                .joinToString("\n")

            assertEquals(publishPodspecCustomContent, actualPodspecContentWithoutBlankLines)
        }

        project.testWithWrapper(":podPublishXCFramework")
    }

    @Test
    fun testPodInstallUpToDateCheck() {
        project = getProjectByName(cocoapodsSingleKtPod)
        val subProjectName = "kotlin-library"
        val subprojectPodImportTask = ":$subProjectName$podImportTaskName"
        val subprojectPodspecTask = ":$subProjectName$podspecTaskName"
        val subprojectPodInstallTask = ":$subProjectName$podInstallTaskName"
        with(project) {
            preparePodfile("ios-app", ImportMode.FRAMEWORKS)
            gradleBuildScript(subProjectName).appendToCocoapodsBlock("ios.deploymentTarget = \"14.0\"")

            build(subprojectPodImportTask, "-Pkotlin.native.cocoapods.generate.wrapper=true") {
                assertTasksExecuted(listOf(subprojectPodspecTask, subprojectPodInstallTask))
            }
            gradleBuildScript(subProjectName).addPod(defaultPodName)
            build(subprojectPodImportTask, "-Pkotlin.native.cocoapods.generate.wrapper=true") {
                assertTasksExecuted(listOf(subprojectPodspecTask, subprojectPodInstallTask))
            }
            build(subprojectPodImportTask, "-Pkotlin.native.cocoapods.generate.wrapper=true") {
                assertTasksNotExecuted(listOf(subprojectPodspecTask, subprojectPodInstallTask))
            }
            addPodToPodfile("ios-app", defaultPodName)
            build(subprojectPodImportTask, "-Pkotlin.native.cocoapods.generate.wrapper=true") {
                assertTasksNotExecuted(listOf(subprojectPodspecTask))
                assertTasksExecuted(listOf(subprojectPodInstallTask))
            }
        }
    }

    @Test
    fun testCinteropKlibsProvideLinkerOptsToFramework() = with(project) {
        gradleBuildScript().addPod("AFNetworking")
        testWithWrapper(":cinteropAFNetworkingIOS")

        val cinteropKlib = projectDir.resolve("build/classes/kotlin/iOS/main/cinterop/cocoapods-cinterop-AFNetworking.klib")
        val manifestLines = ZipFile(cinteropKlib).use { zip ->
            zip.getInputStream(zip.getEntry("default/manifest")).bufferedReader().use { it.readLines() }
        }

        assertContains(manifestLines, "linkerOpts=-framework AFNetworking")
    }

    @Test
    fun testLinkOnlyPods() = with(project) {
        gradleBuildScript().appendToCocoapodsBlock(
            """
            pod("AFNetworking") { linkOnly = true }
            pod("SSZipArchive", linkOnly = true)
            pod("SDWebImage/Core")
        """.trimIndent()
        )

        build(
            ":linkPodDebugFrameworkIOS",
            "-Pkotlin.native.cocoapods.generate.wrapper=true",
        ) {
            assertSuccessful()

            assertTasksExecuted(":podBuildAFNetworkingIphonesimulator")
            assertTasksExecuted(":podBuildSDWebImageIphonesimulator")
            assertTasksExecuted(":podBuildSSZipArchiveIphonesimulator")

            assertTasksExecuted(":cinteropSDWebImageIOS")
            assertTasksNotRegistered(":cinteropAFNetworkingIOS")
            assertTasksNotRegistered(":cinteropSSZipArchiveIOS")

            assertContains(
                """
            |	-linker-option
            |	-framework
            |	-linker-option
            |	AFNetworking
            """.trimMargin()
            )

            assertContains(
                """
            |	-linker-option
            |	-framework
            |	-linker-option
            |	SSZipArchive
            """.trimMargin()
            )
        }
    }

    @Test
    fun testUsageLinkOnlyWithStaticFrameworkProducesMessage() = with(project) {
        gradleBuildScript().appendToCocoapodsBlock(
            """
            framework {
                isStatic = true
            }

            pod("AFNetworking") { linkOnly = true }
        """.trimIndent()
        )

        build(
            ":linkPodDebugFrameworkIOS",
            "-Pkotlin.native.cocoapods.generate.wrapper=true",
        ) {
            assertSuccessful()

            assertContains("Dependency on 'AFNetworking' with option 'linkOnly=true' is unused for building static frameworks")
        }
    }

    @Test
    fun `hierarchy of dependant pods compiles successfully`() = with(getProjectByName("native-cocoapods-dependant-pods")) {
        build(
            ":compileKotlinIosX64",
            "-Pkotlin.native.cocoapods.generate.wrapper=true",
        ) {
            assertSuccessful()
        }
    }

    @Test
    fun `configuration fails when trying to depend on non-declared pod`() = with(getProjectByName("native-cocoapods-dependant-pods")) {
        gradleBuildScript().appendToCocoapodsBlock(
            """
            pod("Foo") { useInteropBindingFrom("JBNonExistent") }
        """.trimIndent()
        )

        build(
            ":help",
            "-Pkotlin.native.cocoapods.generate.wrapper=true",
        ) {
            assertFailed()
            assertContains("Couldn't find declaration of pod 'JBNonExistent' (interop-binding dependency of pod 'Foo')")
        }
    }

    @Test
    fun `configuration fails when dependant pods are in the wrong order`() = with(getProjectByName("native-cocoapods-dependant-pods")) {
        gradleBuildScript().appendToCocoapodsBlock(
            """
            pod("Foo") { useInteropBindingFrom("Bar") }
            pod("Bar")
        """.trimIndent()
        )

        build(
            ":help",
            "-Pkotlin.native.cocoapods.generate.wrapper=true",
        ) {
            assertFailed()
            assertContains("Couldn't find declaration of pod 'Bar' (interop-binding dependency of pod 'Foo')")
        }
    }

    @Test
    fun `configuration fails when pod depends on itself`() = with(getProjectByName("native-cocoapods-dependant-pods")) {
        gradleBuildScript().appendToCocoapodsBlock(
            """
            pod("Foo") { useInteropBindingFrom("Foo") }
        """.trimIndent()
        )

        build(
            ":help",
            "-Pkotlin.native.cocoapods.generate.wrapper=true",
        ) {
            assertFailed()
            assertContains("Pod 'Foo' has an interop-binding dependency on itself")
        }
    }

    // test configuration phase

    private class CustomHooks {
        private val hooks = mutableSetOf<CompiledProject.() -> Unit>()

        fun addHook(hook: CompiledProject.() -> Unit) {
            hooks.add(hook)
        }

        fun rewriteHooks(hook: CompiledProject.() -> Unit) {
            hooks.clear()
            hooks.add(hook)
        }

        fun trigger(project: CompiledProject) {
            hooks.forEach { function ->
                project.function()
            }
        }
    }


    private fun Project.testImportWithAsserts(
        repos: List<String> = listOf(),
        vararg args: String,
    ) {
        hooks.addHook {
            podImportAsserts()
        }
        testImport(repos, *args)
    }

    private fun Project.testImport(
        repos: List<String> = listOf(),
        vararg args: String,
    ) {
        for (repo in repos) {
            assumeTrue(isRepoAvailable(repo))
        }
        testSynthetic(podImportTaskName, *args)
    }

    private fun Project.testSynthetic(
        taskName: String,
        vararg args: String,
    ) {
        testWithWrapper(taskName, *args)
    }

    private fun Project.testWithWrapper(
        taskName: String,
        vararg args: String,
    ) {
        test(taskName, "-Pkotlin.native.cocoapods.generate.wrapper=true", *args)
    }

    private fun Project.test(
        taskName: String,
        vararg args: String,
    ) {

        // check that test executable
        build(taskName, *args) {
            //base checks
            assertSuccessful()
            hooks.trigger(this)
        }
    }

    private fun getProjectByName(projectName: String) = transformProjectWithPluginsDsl(projectName, gradleVersion)


    // build script configuration phase

    private fun File.addPod(podName: String, configuration: String? = null) {
        val pod = "pod(\"$podName\")"
        val podBlock = configuration?.wrap(pod) ?: pod
        appendToCocoapodsBlock(podBlock)
    }

    private fun File.addSpecRepo(specRepo: String) = appendToCocoapodsBlock("url(\"$specRepo\")".wrap("specRepos"))

    private fun File.appendToKotlinBlock(str: String) = appendLine(str.wrap("kotlin"))

    private fun File.appendToCocoapodsBlock(str: String) = appendToKotlinBlock(str.wrap("cocoapods"))

    private fun File.appendToFrameworkBlock(str: String) = appendToCocoapodsBlock(str.wrap("framework"))

    private fun String.wrap(s: String): String = """
        |$s {
        |    $this
        |}
    """.trimMargin()

    private fun File.appendLine(s: String) = appendText("\n$s")


    // proposition phase

    private fun isRepoAvailable(repo: String): Boolean {
        var responseCode = 0
        runCommand(
            File("/"),
            "curl",
            "-s",
            "-o",
            "/dev/null",
            "-w",
            "%{http_code}",
            "-L",
            repo,
            "--retry", "2"
        ) {
            val (retCode, out, errorMessage) = this
            assertEquals(0, retCode, errorMessage)
            responseCode = out.toInt()
        }
        return responseCode == 200
    }

    private fun CompiledProject.podImportAsserts(projectName: String? = null) {

        val buildScriptText = project.gradleBuildScript(projectName).readText()
        val taskPrefix = projectName?.let { ":$it" } ?: ""
        val podspec = "podspec"
        val podInstall = "podInstall"
        assertSuccessful()

        if ("noPodspec()" in buildScriptText) {
            assertTasksSkipped("$taskPrefix:$podspec")
        }

        if ("podfile" in buildScriptText) {
            assertTasksExecuted("$taskPrefix:$podInstall")
        } else {
            assertTasksSkipped("$taskPrefix:$podInstall")
        }
        assertTasksRegisteredByPrefix(listOf("$taskPrefix:$POD_GEN_TASK_NAME"))
        if (buildScriptText.matches("pod\\(.*\\)".toRegex())) {
            assertTasksExecutedByPrefix(listOf("$taskPrefix:$POD_GEN_TASK_NAME"))
        }

        with(listOf(POD_SETUP_BUILD_TASK_NAME, POD_BUILD_TASK_NAME).map { "$taskPrefix:$it" }) {
            if (buildScriptText.matches("pod\\(.*\\)".toRegex())) {
                assertTasksRegisteredByPrefix(this)
                assertTasksExecutedByPrefix(this)
            }
        }
    }

    private data class CommandResult(
        val exitCode: Int,
        val stdOut: String,
        val stdErr: String,
    )

    private fun runCommand(
        workingDir: File,
        command: String,
        vararg args: String,
        timeoutSec: Long = 120,
        inheritIO: Boolean = false,
        block: CommandResult.() -> Unit,
    ) {
        val process = ProcessBuilder(command, *args).apply {
            directory(workingDir)
            environment().putAll(cocoaPodsEnvironmentVariables())
            if (inheritIO) {
                inheritIO()
            }
        }.start()

        val isFinished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
        val stdOut = process.inputStream.bufferedReader().use { it.readText() }
        val stdErr = process.errorStream.bufferedReader().use { it.readText() }

        if (!isFinished) {
            process.destroyForcibly()
            println("Stdout:\n$stdOut")
            println("Stderr:\n$stdErr")
            fail("Command '$command ${args.joinToString(" ")}' killed by timeout.".trimIndent())
        }
        CommandResult(process.exitValue(), stdOut, stdErr).block()
    }

    private fun Project.preparePodfile(iosAppLocation: String, mode: ImportMode) {
        val iosAppDir = projectDir.resolve(iosAppLocation)

        // Set import mode for Podfile.
        iosAppDir.resolve("Podfile").takeIf { it.exists() }?.modify {
            it.replace(podfileImportDirectivePlaceholder, mode.directive)
        }
    }

    private fun Project.addPodToPodfile(iosAppLocation: String, pod: String) {
        val iosAppDir = projectDir.resolve(iosAppLocation)
        iosAppDir.resolve("Podfile").takeIf { it.exists() }?.modify {
            it.replace(podfileImportPodPlaceholder, "pod '$pod'")
        }
    }

    private val publishPodspecContent = """
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
                    spec.ios.deployment_target = '13.5'
                end
            """.trimIndent()

    private val publishPodspecCustomContent = """
                Pod::Spec.new do |spec|
                    spec.name                     = 'CustomPod'
                    spec.version                  = '2.0'
                    spec.homepage                 = 'https://github.com/JetBrains/kotlin'
                    spec.source                   = { :http=> ''}
                    spec.authors                  = { 'Kotlin Dev' => 'kotlin.dev@jetbrains.com' }
                    spec.license                  = 'MIT'
                    spec.summary                  = 'CocoaPods test library'
                    spec.ios.deployment_target = '13.5'
                    spec.dependency 'AFNetworking'
                    spec.social_media_url = 'https://twitter.com/kotlin'
                    spec.vendored_frameworks = 'CustomFramework.xcframework'
                    spec.libraries = 'xml'
                end
            """.trimIndent()

    companion object {
        @BeforeClass
        @JvmStatic
        fun assumeItsMac() {
            assumeTrue(HostManager.hostIsMac)
        }

        @BeforeClass
        @JvmStatic
        fun checkCocoapodsInstalled() {
            if (!HostManager.hostIsMac) {
                return
            }

            ensureCocoapodsInstalled()
        }
    }
}