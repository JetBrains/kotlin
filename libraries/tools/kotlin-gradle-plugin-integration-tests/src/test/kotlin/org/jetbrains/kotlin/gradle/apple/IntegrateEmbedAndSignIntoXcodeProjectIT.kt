/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PbxShellScriptBuildPhase
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodeProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.deserializeXcodeProject
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.test.assertContains
import kotlin.test.assertEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@GradleTestVersions(
    minVersion = TestVersions.Gradle.G_8_0
)
@OptIn(EnvironmentalVariablesOverride::class)
@DisplayName("integrateEmbedAndSign task tests")
@SwiftPMImportGradlePluginTests
class IntegrateEmbedAndSignIntoXcodeProjectIT : KGPBaseTest() {

    @GradleTest
    fun `integrateEmbedAndSign uses root project task path without duplicate separators`(version: GradleVersion) {
        project("emptyxcode-no-embedandsign", version) {
            initDefaultKmpWithLocalSPM()

            val iosAppPath = projectPath.resolve("iosApp")
            val pbxFile = iosAppPath.resolve("iosApp.xcodeproj/project.pbxproj")
            val gradlewPath = projectPath.resolve("gradlew")

            build(
                "integrateEmbedAndSign",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "GRADLEW_PATH" to gradlewPath.absolutePathString(),
                    "GRADLE_PROJECT_PATH" to ":",
                )
            ) {
                val shellScript = pbxFile.kotlinPhaseShellScripts().single()

                assertEquals(
                    """
                        if [ "YES" = "${'$'}OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                          echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                          exit 0
                        fi
                        cd "${'$'}SRCROOT/${gradlewPath.parent.relativeTo(iosAppPath.toRealPath())}"
                        ./gradlew :embedAndSignAppleFrameworkForXcode -i
                    """.trimIndent(),
                    shellScript,
                    message = "Generated embed-and-sign phase should target the root project task path",
                )
            }
        }
    }

    @GradleTest
    fun `integrateEmbedAndSign injects gradle invocation into pbxproj when absent`(version: GradleVersion) {
        project("emptyxcode-no-embedandsign", version) {
            initDefaultKmpWithLocalSPM()

            val pbxFile = projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")
            assertFileDoesNotContain(pbxFile, "./gradlew ")

            build(
                "integrateEmbedAndSign",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "GRADLEW_PATH" to projectPath.resolve("gradlew").absolutePathString(),
                    "GRADLE_PROJECT_PATH" to ":",
                )
            ) {
                val shellScript = pbxFile.kotlinPhaseShellScripts().single()
                assertContains(
                    shellScript,
                    "./gradlew :embedAndSignAppleFrameworkForXcode",
                    message = "Generated `Compile Kotlin Framework` shell script phase should invoke the embedAndSign Gradle task",
                )
            }
        }
    }

    @GradleTest
    fun `integrateEmbedAndSign is idempotent when a gradle script phase already exists`(version: GradleVersion) {
        project("emptyxcode", version) {
            initDefaultKmpWithLocalSPM()

            val pbxFile = projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")
            val pbxBefore = pbxFile.readText()

            build(
                "integrateEmbedAndSign",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "GRADLEW_PATH" to projectPath.resolve("gradlew").absolutePathString(),
                    "GRADLE_PROJECT_PATH" to ":",
                )
            ) {
                assertOutputContains("Found embedAndSign integration. Nothing to do")
                assertEquals(
                    pbxBefore,
                    pbxFile.readText(),
                    "pbxproj must not be modified when an embedAndSign integration already exists",
                )
            }
        }
    }

    @GradleTest
    fun `integrateEmbedAndSign fails when pbxproj has no native targets`(version: GradleVersion) {
        project("emptyxcode-no-embedandsign", version) {
            initDefaultKmpWithLocalSPM()

            // Drop the only PBXNativeTarget by renaming its isa so the native-targets list ends up empty.
            val pbxFile = projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")
            pbxFile.toFile().writeText(
                pbxFile.readText().replace("isa = PBXNativeTarget;", "isa = PBXOpaqueTargetForTest;")
            )

            buildAndFail(
                "integrateEmbedAndSign",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "GRADLEW_PATH" to projectPath.resolve("gradlew").absolutePathString(),
                    "GRADLE_PROJECT_PATH" to ":",
                )
            ) {
                assertOutputContains("Couldn't find targets to insert embedAndSign integration")
            }
        }
    }

    @GradleTest
    fun `integrateEmbedAndSign adds a script phase to every native target when there are multiple`(version: GradleVersion) {
        project("emptyxcode-no-embedandsign", version) {
            initDefaultKmpWithLocalSPM()

            // Inject a second PBXNativeTarget alongside the existing one so the task processes multiple targets.
            val pbxFile = projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")
            val secondTargetEntry = """
                AABBCCDDEEFF112233445566 /* iosAppTwo */ = {
                    isa = PBXNativeTarget;
                    buildPhases = (
                    );
                };
            """.trimIndent()
            pbxFile.toFile().writeText(
                pbxFile.readText().replace(
                    "/* End PBXNativeTarget section */",
                    "$secondTargetEntry/* End PBXNativeTarget section */",
                )
            )

            build(
                "integrateEmbedAndSign",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "GRADLEW_PATH" to projectPath.resolve("gradlew").absolutePathString(),
                    "GRADLE_PROJECT_PATH" to ":",
                )
            ) {
                val xcodeProject: XcodeProject = pbxFile.readPbxprojAsXcodeProject()
                val compileKotlinFrameworkPhases = xcodeProject.objects.values
                    .filterIsInstance<PbxShellScriptBuildPhase>()
                    .filter { it.name == "Compile Kotlin Framework" }
                assertEquals(
                    2, compileKotlinFrameworkPhases.size,
                    "Expected one `Compile Kotlin Framework` phase per PBXNativeTarget, got ${compileKotlinFrameworkPhases.size}",
                )
            }
        }
    }

    @GradleTest
    fun `integrateEmbedAndSign fails when GRADLE_PROJECT_PATH env var is missing`(version: GradleVersion) {
        project("emptyxcode-no-embedandsign", version) {
            initDefaultKmpWithLocalSPM()

            buildAndFail(
                "integrateEmbedAndSign",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "GRADLEW_PATH" to projectPath.resolve("gradlew").absolutePathString(),
                )
            ) {
                assertOutputContains("Please specify path to gradle project in GRADLE_PROJECT_PATH environment variable")
            }
        }
    }
}

private fun TestProject.initDefaultKmpWithLocalSPM() {
    val localSwiftPackageRelativePath = "../localSwiftPackage"
    createLocalSwiftPackage(projectPath.resolve(localSwiftPackageRelativePath))

    initDefaultKmp {
        swiftPMDependencies {
            localSwiftPackage(
                directory = project.layout.projectDirectory.dir(localSwiftPackageRelativePath),
                products = listOf("LocalSwiftPackage"),
            )
        }
    }
}

private fun Path.readPbxprojAsXcodeProject(): XcodeProject {
    val result = runProcess(
        cmd = listOf("/usr/bin/plutil", "-convert", "json", absolutePathString(), "-o", "-"),
        workingDir = parent.toFile(),
        redirectErrorStream = false,
    )
    check(result.isSuccessful) {
        "plutil failed (exit=${result.exitCode}) on $this: ${result.stdErr}"
    }
    return deserializeXcodeProject(result.output.toByteArray())
}

private fun Path.kotlinPhaseShellScripts(): List<String> {
    val xcodeProject: XcodeProject = readPbxprojAsXcodeProject()
    return xcodeProject.objects.values
        .filterIsInstance<PbxShellScriptBuildPhase>()
        .filter { it.name == "Compile Kotlin Framework" }
        .map { it.shellScript?.stringValue.orEmpty() }
}
