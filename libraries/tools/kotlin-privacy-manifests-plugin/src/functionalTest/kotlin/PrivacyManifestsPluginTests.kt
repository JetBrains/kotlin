package org.jetbrains.kotlin

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createFile


class PrivacyManifestsPluginTests {

    companion object {
        @JvmStatic
        fun kotlinVersions(): List<String> {
            val installedKotlinVersion = System.getProperty("kotlinVersion")
            if (installedKotlinVersion.isNullOrEmpty()) {
                error("Installed Kotlin version isn't specified")
            }
            return listOf(
                "1.9.23",
                "2.0.0-RC1",
                installedKotlinVersion,
            )
        }
    }

    @ParameterizedTest
    @MethodSource("kotlinVersions")
    fun `embedAndSign integration - with macOS bundling`(kotlinVersion: String) {
        testEmbedAndSignIntegration(
            target = "macosArm64",
            appName = "My.app",
            frameworks = "Contents/Frameworks",
            frameworksFolderPath = "My.app/Contents/Frameworks",
            resourcesFolderPath = "My.app/Contents/Resources",
            sdkName = "macosx",
            expectedPrivacyManifestPlacement = "My.app/Contents/Resources/KotlinMultiplatformPrivacyManifest.bundle/Contents/Resources/PrivacyInfo.xcprivacy",
            expectedInfoPlistPlacement = "My.app/Contents/Resources/KotlinMultiplatformPrivacyManifest.bundle/Contents/Info.plist",
            expectedInfoPlistContent = "info",
            infoPlistFile = "info",
            kotlinVersion = kotlinVersion
        )
    }

    @ParameterizedTest
    @MethodSource("kotlinVersions")
    fun `embedAndSign integration - with iOS bundling`(kotlinVersion: String) {
        testEmbedAndSignIntegration(
            target = "iosArm64",
            appName = "My.app",
            frameworks = "Frameworks",
            frameworksFolderPath = "My.app/Frameworks",
            resourcesFolderPath = "My.app",
            sdkName = "iphoneos",
            expectedPrivacyManifestPlacement = "My.app/KotlinMultiplatformPrivacyManifest.bundle/PrivacyInfo.xcprivacy",
            expectedInfoPlistPlacement = "My.app/KotlinMultiplatformPrivacyManifest.bundle/Info.plist",
            expectedInfoPlistContent = "info",
            infoPlistFile = "info",
            kotlinVersion = kotlinVersion,
        )
    }

    @Test
    fun `embedAndSign integration - without explicit plist`() {
        testEmbedAndSignIntegration(
            target = "iosArm64",
            testNameSuffix = "_explicitPlist",
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
            kotlinVersion = kotlinVersions()[0],
        )
    }

    @Test
    fun `embedAndSign integration - with disabled plugin`() {
        testEmbedAndSignIntegration(
            target = "iosArm64",
            testNameSuffix = "_disabled",
            appName = "My.app",
            frameworks = "Frameworks",
            frameworksFolderPath = "My.app/Frameworks",
            resourcesFolderPath = "My.app",
            sdkName = "iphoneos",
            expectedPrivacyManifestPlacement = "My.app/KotlinMultiplatformPrivacyManifest.bundle/PrivacyInfo.xcprivacy",
            expectedInfoPlistPlacement = "My.app/KotlinMultiplatformPrivacyManifest.bundle/Info.plist",
            expectedInfoPlistContent = "info",
            infoPlistFile = "info",
            additionalConfiguration = "extensions.extraProperties.set(\"kotlin.mpp.disablePrivacyManifestsPlugin\", \"1\")",
            kotlinVersion = kotlinVersions()[0],
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
        target: String,
        testNameSuffix: String = "",
        appName: String,
        frameworks: String,
        frameworksFolderPath: String,
        resourcesFolderPath: String,
        sdkName: String,
        expectedPrivacyManifestPlacement: String,
        expectedInfoPlistPlacement: String,
        expectedInfoPlistContent: String,
        infoPlistFile: String?,
        kotlinVersion: String,
        additionalConfiguration: String = "",
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
        val testName = "embedAndSign_${target}_${kotlinVersion}${testNameSuffix}".pathFriendly()
        val projectDir = File("build/functionalTest").resolve(testName)
        val embedAndSignOutputs = projectDir.resolve("embedAndSignOutputs")
        embedAndSignOutputs.resolve(appName).resolve(frameworks).mkdirs()

        buildAndTestProject(
            projectDir = projectDir,
            kotlinVersion = kotlinVersion,
            buildScript = """
                kotlin {
                    ${target} {
                        binaries.framework {}
                    }
                    
                    ${additionalConfiguration}
                    
                    privacyManifest {
                        embed(
                            privacyManifest = layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile,
                            resourceBundleInfoPlist = ${infoPlistFile?.let { "layout.projectDirectory.file(\"Info.plist\").asFile" } ?: "null"},
                        )
                    }
                }
            """.trimIndent(),
            gradleArguments = arrayOf(
                ":embedAndSign",
            ),
            environmentVariables = System.getenv() + mapOf(
                "ENABLE_USER_SCRIPT_SANDBOXING" to "NO",
                "CONFIGURATION" to "Debug",
                "ARCHS" to "arm64",
                "SDK_NAME" to sdkName,
                "FRAMEWORKS_FOLDER_PATH" to frameworksFolderPath,
                "UNLOCALIZED_RESOURCES_FOLDER_PATH" to resourcesFolderPath,
                "TARGET_BUILD_DIR" to embedAndSignOutputs.canonicalPath,
                "BUILT_PRODUCTS_DIR" to embedAndSignOutputs.canonicalPath,
            ),
            infoPlistFile = infoPlistFile,
            beforeMutatingPrivacyManifest = {
                beforeMutatingPrivacyManifest(embedAndSignOutputs, expectedPrivacyManifestPlacement, expectedInfoPlistPlacement)
            },
            afterMutatingPrivacyManifest = {
                afterMutatingPrivacyManifest(embedAndSignOutputs, expectedPrivacyManifestPlacement, expectedInfoPlistPlacement)
            }
        )
    }

    @ParameterizedTest
    @MethodSource("kotlinVersions")
    fun `xcframework creation`(kotlinVersion: String) {
        val testName = "xcframework_${kotlinVersion}".pathFriendly()
        val projectDir = File("build/functionalTest").resolve(testName)

        buildAndTestProject(
            projectDir = projectDir,
            kotlinVersion = kotlinVersion,
            buildScript = """
                import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
                kotlin {
                    val xcf = XCFramework()
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
                    
                    privacyManifest {
                        embed(
                            layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile
                        )
                    }
                }
            """.trimIndent(),
            gradleArguments = arrayOf(
                ":assembleXCFramework",
            ),
            beforeMutatingPrivacyManifest = {
                assertEquals(
                    "initial",
                    projectDir.resolve("build/XCFrameworks/release/${testName}.xcframework/ios-arm64/${testName}.framework/PrivacyInfo.xcprivacy")
                        .readText(),
                )
                assertEquals(
                    "initial",
                    projectDir.resolve("build/XCFrameworks/release/${testName}.xcframework/watchos-arm64_x86_64-simulator/${testName}.framework/PrivacyInfo.xcprivacy")
                        .readText(),
                )
                assertEquals(
                    "initial",
                    projectDir.resolve("build/XCFrameworks/release/${testName}.xcframework/macos-arm64_x86_64/${testName}.framework/Resources/PrivacyInfo.xcprivacy")
                        .readText(),
                )
            },
            afterMutatingPrivacyManifest = {
                assertEquals(
                    "final",
                    projectDir.resolve("build/XCFrameworks/release/${testName}.xcframework/ios-arm64/${testName}.framework/PrivacyInfo.xcprivacy")
                        .readText(),
                )
            }
        )
    }

    @ParameterizedTest
    @MethodSource("kotlinVersions")
    fun `xcframework creation with CocoaPods`(kotlinVersion: String) {
        val testName = "xcframeworkCocoaPods_${kotlinVersion}".pathFriendly()
        val projectDir = File("build/functionalTest").resolve(testName)

        buildAndTestProject(
            projectDir = projectDir,
            kotlinVersion = kotlinVersion,
            otherPlugins = """
                kotlin("native.cocoapods") version "$kotlinVersion"
            """.trimIndent(),
            buildScript = """
                import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
                kotlin {
                    // Thin
                    iosArm64()
            
                    // Universal
                    watchosSimulatorArm64()
                    watchosX64()
                    
                    // Thin macOS
                    macosArm64()
                    
                    cocoapods {
                        version = "1.0"
                    }
                    
                    privacyManifest {
                        embed(
                            layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile
                        )
                    }
                }
            """.trimIndent(),
            gradleArguments = arrayOf(
                ":podPublishReleaseXCFramework",
            ),
            beforeMutatingPrivacyManifest = {
                assertEquals(
                    "initial",
                    projectDir.resolve("build/cocoapods/publish/release/${testName}.xcframework/ios-arm64/${testName}.framework/PrivacyInfo.xcprivacy")
                        .readText(),
                )
                assertEquals(
                    "initial",
                    projectDir.resolve("build/cocoapods/publish/release/${testName}.xcframework/watchos-arm64_x86_64-simulator/${testName}.framework/PrivacyInfo.xcprivacy")
                        .readText(),
                )
                assertEquals(
                    "initial",
                    projectDir.resolve("build/cocoapods/publish/release/${testName}.xcframework/macos-arm64/${testName}.framework/Resources/PrivacyInfo.xcprivacy")
                        .readText(),
                )
            },
            afterMutatingPrivacyManifest = {
                assertEquals(
                    "final",
                    projectDir.resolve("build/cocoapods/publish/release/${testName}.xcframework/ios-arm64/${testName}.framework/PrivacyInfo.xcprivacy")
                        .readText(),
                )
            }
        )
    }

    @ParameterizedTest
    @MethodSource("kotlinVersions")
    fun `CocoaPods syncFramework integration`(kotlinVersion: String) {
        val testName = "cocoaPodsSyncFramework_${kotlinVersion}".pathFriendly()
        val projectDir = File("build/functionalTest").resolve(testName)

        buildAndTestProject(
            projectDir = projectDir,
            kotlinVersion = kotlinVersion,
            otherPlugins = """
                kotlin("native.cocoapods") version "$kotlinVersion"
            """.trimIndent(),
            buildScript = """
                import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
                kotlin {
                    // Thin
                    iosArm64()
            
                    // Universal
                    watchosSimulatorArm64()
                    watchosX64()
                    
                    cocoapods {
                        version = "1.0"
                    }
                    
                    privacyManifest {
                        embed(
                            layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile
                        )
                    }
                }
            """.trimIndent(),
            gradleArguments = arrayOf(
                ":podspec",
                ":syncFramework",
                "-Pkotlin.native.cocoapods.configuration=Debug",
                "-Pkotlin.native.cocoapods.archs=arm64",
                "-Pkotlin.native.cocoapods.platform=iphoneos",
                "-Pkotlin.native.cocoapods.generate.wrapper=true",
            ),
            beforeMutatingPrivacyManifest = {
                assert(
                    projectDir.resolve("build/cocoapods/framework/${testName}.framework").exists(),
                    { "syncFramework integration is expected to create a framework" },
                )
                assert(
                    !projectDir.resolve("build/cocoapods/framework/${testName}.framework/PrivacyInfo.xcprivacy").exists(),
                    { "Privacy manifest shouldn't be copied into framework for syncFramework integration" },
                )
                assert(
                    projectDir.resolve("${testName}.podspec").readText()
                        .contains("spec.resource_bundles = {'KotlinMultiplatformPrivacyManifest' => ['PrivacyInfo.xcprivacy']}"),
                    { "syncFramework integration is expected to create an resource bundle entry in the local integration podspec" },
                )
            },
            afterMutatingPrivacyManifest = {}
        )
    }

    private fun buildAndTestProject(
        projectDir: File,
        kotlinVersion: String,
        otherPlugins: String = "",
        buildScript: String,
        gradleArguments: Array<String>,
        environmentVariables: Map<String, String> = emptyMap(),
        infoPlistFile: String? = null,
        beforeMutatingPrivacyManifest: () -> (Unit),
        afterMutatingPrivacyManifest: () -> (Unit),
    ) {
        Files.createDirectories(projectDir.toPath())

        projectDir.resolve("settings.gradle.kts").writeText(
            """
                dependencyResolutionManagement {
                    repositories {
                        mavenLocal()
                        mavenCentral()
                        ivy {
                            url = uri("https://download.jetbrains.com/kotlin/native/builds/dev")
                            patternLayout {
                                artifact("[revision]/[classifier]/[artifact]-[classifier]-[revision].[ext]")
                            }
                            metadataSources {
                                artifact()
                            }
                        }
                    }
                }
                
                pluginManagement {
                    repositories {
                        mavenLocal()
                        maven("file://${projectDir.absoluteFile.parentFile.parentFile.parentFile.resolve("build/repo").canonicalPath}")
                        gradlePluginPortal()
                    }
                }
            """.trimIndent()
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
                plugins {
                    kotlin("apple-privacy-manifests") version "test"
                    kotlin("multiplatform") version "$kotlinVersion"
                    $otherPlugins
                }

                $buildScript
            """.trimIndent()
        )

        val privacyManifestFile = File(projectDir, "PrivacyInfo.xcprivacy")
        privacyManifestFile.writeText(
            "initial"
        )
        infoPlistFile?.let {
            File(projectDir, "Info.plist").writeText(it)
        }

        val sources = projectDir.resolve("src/commonMain/kotlin")
        if (!sources.exists()) {
            sources.mkdirs()
            sources.resolve("stub.kt").toPath().createFile()
        }

        val build = {
            GradleRunner.create()
                .forwardOutput()
                .withEnvironment(System.getenv() + environmentVariables)
                .withArguments(
                    *(gradleArguments + arrayOf(
                        "--info",
                        "--stacktrace",
                        "--configuration-cache",
                        "-Dorg.gradle.jvmargs=-XX:MaxMetaspaceSize=1g"
                    ))
                )
                .withProjectDir(projectDir)
                .build()
        }

        build()
        beforeMutatingPrivacyManifest()

        privacyManifestFile.writeText(
            "final"
        )

        build()
        afterMutatingPrivacyManifest()
    }

    private fun String.pathFriendly() = replace("-", "_")

}