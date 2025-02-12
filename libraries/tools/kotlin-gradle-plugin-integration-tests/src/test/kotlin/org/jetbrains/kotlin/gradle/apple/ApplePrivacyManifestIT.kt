/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.PrivacyManifest
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(EnvironmentalVariablesOverride::class)
@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@NativeGradlePluginTests
class ApplePrivacyManifestIT : KGPBaseTest() {

    @GradleTest
    fun `embedAndSign integration - with macOS bundling`(
        gradleVersion: GradleVersion,
        @TempDir temp: File,
    ) {
        testEmbedAndSignIntegration(
            gradleVersion = gradleVersion,
            target = { macosArm64() },
            temp = temp,
            appName = "My.app",
            frameworks = "Contents/Frameworks",
            frameworksFolderPath = "My.app/Contents/Frameworks",
            resourcesFolderPath = "My.app/Contents/Resources",
            sdkName = "macosx",
            expectedPrivacyManifestPlacement = "My.app/Contents/Resources/KotlinMultiplatformPrivacyManifest.bundle/Contents/Resources/PrivacyInfo.xcprivacy",
            expectedInfoPlistPlacement = "My.app/Contents/Resources/KotlinMultiplatformPrivacyManifest.bundle/Contents/Info.plist",
            expectedInfoPlistContent = "info",
            infoPlistFile = "info",
        )
    }

    @GradleTest
    fun `embedAndSign integration - with iOS bundling`(
        gradleVersion: GradleVersion,
        @TempDir temp: File,
    ) {
        testEmbedAndSignIntegration(
            gradleVersion = gradleVersion,
            target = { iosArm64() },
            temp = temp,
            appName = "My.app",
            frameworks = "Frameworks",
            frameworksFolderPath = "My.app/Frameworks",
            resourcesFolderPath = "My.app",
            sdkName = "iphoneos",
            expectedPrivacyManifestPlacement = "My.app/KotlinMultiplatformPrivacyManifest.bundle/PrivacyInfo.xcprivacy",
            expectedInfoPlistPlacement = "My.app/KotlinMultiplatformPrivacyManifest.bundle/Info.plist",
            expectedInfoPlistContent = "info",
            infoPlistFile = "info",
        )
    }

    @GradleTest
    fun `embedAndSign integration - without explicit plist`(
        gradleVersion: GradleVersion,
        @TempDir temp: File,
    ) {
        testEmbedAndSignIntegration(
            gradleVersion = gradleVersion,
            target = { iosArm64() },
            temp = temp,
            appName = "My.app",
            frameworks = "Frameworks",
            frameworksFolderPath = "My.app/Frameworks",
            resourcesFolderPath = "My.app",
            sdkName = "iphoneos",
            expectedPrivacyManifestPlacement = "My.app/KotlinMultiplatformPrivacyManifest.bundle/PrivacyInfo.xcprivacy",
            expectedInfoPlistPlacement = "My.app/KotlinMultiplatformPrivacyManifest.bundle/Info.plist",
            expectedInfoPlistContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0">
                <dict>
                    <key>CFBundleIdentifier</key>
                    <string>KotlinMultiplatformPrivacyManifest-resources</string>
                    <key>CFBundleName</key>
                    <string>KotlinMultiplatformPrivacyManifest</string>
                    <key>CFBundleInfoDictionaryVersion</key>
                    <string>6.0</string>
                    <key>CFBundlePackageType</key>
                    <string>BNDL</string>
                    <key>CFBundleShortVersionString</key>
                    <string>1.0</string>
                </dict>
                </plist>
            """.trimIndent(),
            infoPlistFile = null,
        )
    }

    @GradleTest
    fun `embedAndSign integration - with disabled plugin`(
        gradleVersion: GradleVersion,
        @TempDir temp: File,
    ) {
        testEmbedAndSignIntegration(
            gradleVersion = gradleVersion,
            target = { iosArm64() },
            temp = temp,
            appName = "My.app",
            frameworks = "Frameworks",
            frameworksFolderPath = "My.app/Frameworks",
            resourcesFolderPath = "My.app",
            sdkName = "iphoneos",
            expectedPrivacyManifestPlacement = "My.app/KotlinMultiplatformPrivacyManifest.bundle/PrivacyInfo.xcprivacy",
            expectedInfoPlistPlacement = "My.app/KotlinMultiplatformPrivacyManifest.bundle/Info.plist",
            expectedInfoPlistContent = "info",
            infoPlistFile = "info",
            prePluginConfiguration = { project.extensions.extraProperties.set("kotlin.mpp.disablePrivacyManifestsPlugin", "1") },
            beforeMutatingPrivacyManifest = { embedAndSignOutputs, expectedPrivacyManifestPlacement, expectedInfoPlistPlacement ->
                assert(
                    !embedAndSignOutputs.resolve(expectedPrivacyManifestPlacement).exists()
                )
                assert(
                    !embedAndSignOutputs.resolve(expectedInfoPlistPlacement).exists()
                )
            },
            afterMutatingPrivacyManifest = { _, _, _ -> }
        )
    }

    private fun testEmbedAndSignIntegration(
        gradleVersion: GradleVersion,
        target: KotlinMultiplatformExtension.() -> (KotlinNativeTarget),
        appName: String,
        frameworks: String,
        temp: File,
        frameworksFolderPath: String,
        resourcesFolderPath: String,
        sdkName: String,
        expectedPrivacyManifestPlacement: String,
        expectedInfoPlistPlacement: String,
        expectedInfoPlistContent: String,
        infoPlistFile: String?,
        prePluginConfiguration: GradleProjectBuildScriptInjectionContext.() -> (Unit) = {},
        beforeMutatingPrivacyManifest: (File, String, String) -> (Unit) = { embedAndSignOutputs, expectedPrivacyManifestPlacement, expectedInfoPlistPlacement ->
            assertEquals(
                "initial",
                embedAndSignOutputs.resolve(expectedPrivacyManifestPlacement).readText(),
            )
            assertEquals(
                "initial",
                embedAndSignOutputs.resolve(expectedPrivacyManifestPlacement).readText(),
            )
            assertEquals(
                expectedInfoPlistContent,
                embedAndSignOutputs.resolve(expectedInfoPlistPlacement).readText(),
            )
        },
        afterMutatingPrivacyManifest: (File, String, String) -> (Unit) = { embedAndSignOutputs, expectedPrivacyManifestPlacement, _ ->
            assertEquals(
                "final",
                embedAndSignOutputs.resolve(expectedPrivacyManifestPlacement).readText(),
            )
        },
    ) {
        val embedAndSignOutputs = temp.resolve("embedAndSignOutputs").resolve(appName).resolve(frameworks)
        buildAndTestProject(
            gradleVersion = gradleVersion,
            prePluginConfiguration = prePluginConfiguration,
            buildScript = {
                project.applyMultiplatform {
                    target().binaries.framework {}

                    with(extensions.getByType(PrivacyManifest::class.java)) {
                        embed(
                            privacyManifest = project.layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile,
                            resourceBundleInfoPlist = infoPlistFile?.let { project.layout.projectDirectory.file("Info.plist").asFile },
                        )
                    }
                }
            },
            gradleArguments = arrayOf(
                ":embedAndSign",
            ),
            environmentVariables = mapOf(
                "ENABLE_USER_SCRIPT_SANDBOXING" to "NO",
                "CONFIGURATION" to "Debug",
                "ARCHS" to "arm64",
                "SDK_NAME" to sdkName,
                "FRAMEWORKS_FOLDER_PATH" to frameworksFolderPath,
                "UNLOCALIZED_RESOURCES_FOLDER_PATH" to resourcesFolderPath,
                "TARGET_BUILD_DIR" to embedAndSignOutputs.canonicalPath,
                "BUILT_PRODUCTS_DIR" to embedAndSignOutputs.canonicalPath,
            ),
            infoPlistFileContent = infoPlistFile,
            beforeMutatingPrivacyManifest = {
                beforeMutatingPrivacyManifest(embedAndSignOutputs, expectedPrivacyManifestPlacement, expectedInfoPlistPlacement)
            },
            afterMutatingPrivacyManifest = {
                afterMutatingPrivacyManifest(embedAndSignOutputs, expectedPrivacyManifestPlacement, expectedInfoPlistPlacement)
            }
        )
    }

    @GradleTest
    fun `xcframework creation`(gradleVersion: GradleVersion) {
        buildAndTestProject(
            gradleVersion = gradleVersion,
            buildScript = {
                project.applyMultiplatform {
                    val xcf = project.XCFramework()
                    listOf(
                        // Thin
                        iosArm64(),

                        // Universal
                        watchosSimulatorArm64(),
                        watchosX64(),

                        // Universal macOS
                        macosArm64(),
                        macosX64(),
                    ).forEach {
                        it.binaries.framework {
                            xcf.add(this)
                        }
                    }

                    with(extensions.getByType(PrivacyManifest::class.java)) {
                        embed(
                            project.layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile
                        )
                    }
                }
            },
            gradleArguments = arrayOf(
                ":assembleEmptyDebugXCFramework",
            ),
            beforeMutatingPrivacyManifest = { projectDir ->
                assertEquals(
                    "initial",
                    projectDir.resolve("build/XCFrameworks/debug/empty.xcframework/ios-arm64/empty.framework/PrivacyInfo.xcprivacy")
                        .readText(),
                )
                assertEquals(
                    "initial",
                    projectDir.resolve("build/XCFrameworks/debug/empty.xcframework/watchos-arm64_x86_64-simulator/empty.framework/PrivacyInfo.xcprivacy")
                        .readText(),
                )
                assertEquals(
                    "initial",
                    projectDir.resolve("build/XCFrameworks/debug/empty.xcframework/macos-arm64_x86_64/empty.framework/Resources/PrivacyInfo.xcprivacy")
                        .readText(),
                )
            },
            afterMutatingPrivacyManifest = { projectDir ->
                assertEquals(
                    "final",
                    projectDir.resolve("build/XCFrameworks/debug/empty.xcframework/ios-arm64/empty.framework/PrivacyInfo.xcprivacy")
                        .readText(),
                )
            }
        )
    }

    @GradleTest
    fun `xcframework creation with CocoaPods`(gradleVersion: GradleVersion) {
        buildAndTestProject(
            gradleVersion = gradleVersion,
            buildScript = {
                project.plugins.apply("org.jetbrains.kotlin.native.cocoapods")
                project.applyMultiplatform {
                    // Thin
                    iosArm64()

                    // Universal
                    watchosSimulatorArm64()
                    watchosX64()

                    // Thin macOS
                    macosArm64()

                    with(cocoapods) {
                        version = "1.0"
                    }

                    with(extensions.getByType(PrivacyManifest::class.java)) {
                        embed(
                            project.layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile
                        )
                    }
                }
            },
            gradleArguments = arrayOf(
                ":podPublishDebugXCFramework",
            ),
            beforeMutatingPrivacyManifest = { projectDir ->
                assertEquals(
                    "initial",
                    projectDir.resolve("build/cocoapods/publish/debug/empty.xcframework/ios-arm64/empty.framework/PrivacyInfo.xcprivacy")
                        .readText(),
                )
                assertEquals(
                    "initial",
                    projectDir.resolve("build/cocoapods/publish/debug/empty.xcframework/watchos-arm64_x86_64-simulator/empty.framework/PrivacyInfo.xcprivacy")
                        .readText(),
                )
                assertEquals(
                    "initial",
                    projectDir.resolve("build/cocoapods/publish/debug/empty.xcframework/macos-arm64/empty.framework/Resources/PrivacyInfo.xcprivacy")
                        .readText(),
                )
            },
            afterMutatingPrivacyManifest = { projectDir ->
                assertEquals(
                    "final",
                    projectDir.resolve("build/cocoapods/publish/debug/empty.xcframework/ios-arm64/empty.framework/PrivacyInfo.xcprivacy")
                        .readText(),
                )
            }
        )
    }

    @GradleTest
    fun `CocoaPods syncFramework integration`(gradleVersion: GradleVersion) {
        buildAndTestProject(
            gradleVersion = gradleVersion,
            buildScript = {
                project.plugins.apply("org.jetbrains.kotlin.native.cocoapods")
                project.applyMultiplatform {
                    // Thin
                    iosArm64()

                    // Universal
                    watchosSimulatorArm64()
                    watchosX64()

                    with(cocoapods) {
                        version = "1.0"
                    }

                    with(extensions.getByType(PrivacyManifest::class.java)) {
                        embed(
                            project.layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile
                        )
                    }
                }
            },
            gradleArguments = arrayOf(
                ":podspec",
                ":syncFramework",
                "-Pkotlin.native.cocoapods.configuration=Debug",
                "-Pkotlin.native.cocoapods.archs=arm64",
                "-Pkotlin.native.cocoapods.platform=iphoneos",
                "-Pkotlin.native.cocoapods.generate.wrapper=true",
            ),
            beforeMutatingPrivacyManifest = { projectDir ->
                assert(
                    projectDir.resolve("build/cocoapods/framework/empty.framework").exists(),
                    { "syncFramework integration is expected to create a framework" },
                )
                assert(
                    !projectDir.resolve("build/cocoapods/framework/empty.framework/PrivacyInfo.xcprivacy").exists(),
                    { "Privacy manifest shouldn't be copied into framework for syncFramework integration" },
                )
                assert(
                    projectDir.resolve("empty.podspec").readText()
                        .contains("spec.resource_bundles = {'KotlinMultiplatformPrivacyManifest' => ['PrivacyInfo.xcprivacy']}"),
                    { "syncFramework integration is expected to create an resource bundle entry in the local integration podspec" },
                )
            },
            afterMutatingPrivacyManifest = {}
        )
    }

    private fun buildAndTestProject(
        gradleVersion: GradleVersion,
        prePluginConfiguration: GradleProjectBuildScriptInjectionContext.() -> Unit = {},
        buildScript: GradleProjectBuildScriptInjectionContext.() -> Unit,
        gradleArguments: Array<String>,
        environmentVariables: Map<String, String> = emptyMap(),
        infoPlistFileContent: String? = null,
        beforeMutatingPrivacyManifest: (File) -> (Unit),
        afterMutatingPrivacyManifest: (File) -> (Unit),
    ) {
        val project = project("empty", gradleVersion) {
            val applePrivacyManifestClasspath = System.getProperty("applePrivacyManifestPluginClasspath").split(":").map {
                File(it)
            }
            addKgpToBuildScriptCompilationClasspath()
            buildScriptBuildscriptBlockInjection {
                buildscript.configurations.getByName("classpath").dependencies.add(
                    buildscript.dependencies.create(project.files(applePrivacyManifestClasspath))
                )
            }

            buildScriptInjection {
                prePluginConfiguration()
                project.plugins.apply("org.jetbrains.kotlin.apple-privacy-manifests")
                project.applyMultiplatform {
                    sourceSets.commonMain.get().compileSource("class Common")
                }
                buildScript()
            }
        }

        val privacyManifestFile = project.projectPath.resolve("PrivacyInfo.xcprivacy").toFile()
        privacyManifestFile.writeText(
            "initial"
        )
        infoPlistFileContent?.let {
            project.projectPath.resolve("Info.plist").toFile().writeText(it)
        }

        project.build(
            *gradleArguments,
            environmentVariables = EnvironmentalVariables(environmentVariables)
        )
        beforeMutatingPrivacyManifest(project.projectPath.toFile())

        privacyManifestFile.writeText(
            "final"
        )

        project.build(
            *gradleArguments,
            environmentVariables = EnvironmentalVariables(environmentVariables)
        )
        afterMutatingPrivacyManifest(project.projectPath.toFile())
    }
}