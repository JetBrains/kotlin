/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.tooling.BuildKotlinToolingMetadataTask
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.*
import kotlin.test.*

@DisplayName("KMP/Android integration")
@AndroidGradlePluginTests
class KotlinAndroidMppIT : KGPBaseTest() {

    @DisplayName("KotlinToolingMetadataArtifact is bundled into apk")
    @GradleAndroidTest
    fun testKotlinToolingMetadataBundle(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "kotlinToolingMetadataAndroid",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertTasksAreNotInTaskGraph(":${BuildKotlinToolingMetadataTask.defaultTaskName}")

                val debugApk = projectPath.resolve("build/outputs/apk/debug/project-debug.apk")
                assertFileExists(debugApk)
                ZipFile(debugApk.toFile()).use { zip ->
                    assertNull(zip.getEntry("kotlin-tooling-metadata.json"), "Expected metadata *not* being packaged into debug apk")
                }
            }

            build(
                "clean", "assembleRelease",
            ) {
                assertTasksExecuted(":${BuildKotlinToolingMetadataTask.defaultTaskName}")
                val releaseApk = projectPath.resolve("build/outputs/apk/release/project-release-unsigned.apk")

                assertFileExists(releaseApk)
                ZipFile(releaseApk.toFile()).use { zip ->
                    assertNotNull(zip.getEntry("kotlin-tooling-metadata.json"), "Expected metadata being packaged into release apk")
                }
            }
        }
    }

    @DisplayName("mpp source sets are registered in AGP")
    @GradleAndroidTest
    fun testAndroidMppSourceSets(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android-source-sets",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion,
                // AGP's SourceSetsTask is not CC compatible
                // see https://issuetracker.google.com/issues/242872035
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED,
            ),
            buildJdk = jdkVersion.location
        ) {
            build("sourceSets") {
                fun assertOutputContainsOsIndependent(expectedString: String) {
                    assertOutputContains(expectedString.replace("/", File.separator))
                }
                assertOutputContainsOsIndependent("Android resources: [lib/src/main/res, lib/src/androidMain/res]")
                assertOutputContainsOsIndependent("Assets: [lib/src/main/assets, lib/src/androidMain/assets]")
                assertOutputContainsOsIndependent("AIDL sources: [lib/src/main/aidl, lib/src/androidMain/aidl]")
                assertOutputContainsOsIndependent("RenderScript sources: [lib/src/main/rs, lib/src/androidMain/rs]")
                assertOutputContainsOsIndependent("JNI sources: [lib/src/main/jni, lib/src/androidMain/jni]")
                assertOutputContainsOsIndependent("JNI libraries: [lib/src/main/jniLibs, lib/src/androidMain/jniLibs]")
                assertOutputContainsOsIndependent("Java-style resources: [lib/src/main/resources, lib/src/androidMain/resources]")

                assertOutputContainsOsIndependent("Android resources: [lib/src/androidTestDebug/res, lib/src/androidInstrumentedTestDebug/res]")
                assertOutputContainsOsIndependent("Assets: [lib/src/androidTestDebug/assets, lib/src/androidInstrumentedTestDebug/assets]")
                assertOutputContainsOsIndependent("AIDL sources: [lib/src/androidTestDebug/aidl, lib/src/androidInstrumentedTestDebug/aidl]")
                assertOutputContainsOsIndependent("RenderScript sources: [lib/src/androidTestDebug/rs, lib/src/androidInstrumentedTestDebug/rs]")
                assertOutputContainsOsIndependent("JNI sources: [lib/src/androidTestDebug/jni, lib/src/androidInstrumentedTestDebug/jni]")
                assertOutputContainsOsIndependent("JNI libraries: [lib/src/androidTestDebug/jniLibs, lib/src/androidInstrumentedTestDebug/jniLibs]")
                assertOutputContainsOsIndependent("Java-style resources: [lib/src/androidTestDebug/resources, lib/src/androidInstrumentedTestDebug/resources]")

                assertOutputContainsOsIndependent("Java-style resources: [lib/betaSrc/paidBeta/resources, lib/src/androidPaidBeta/resources]")
                assertOutputContainsOsIndependent("Java-style resources: [lib/betaSrc/paidBetaDebug/resources, lib/src/androidPaidBetaDebug/resources]")
                assertOutputContainsOsIndependent("Java-style resources: [lib/betaSrc/paidBetaRelease/resources, lib/src/androidPaidBetaRelease/resources]")

                assertOutputContainsOsIndependent("Java-style resources: [lib/betaSrc/freeBeta/resources, lib/src/androidFreeBeta/resources]")
                assertOutputContainsOsIndependent("Java-style resources: [lib/betaSrc/freeBetaDebug/resources, lib/src/androidFreeBetaDebug/resources]")
                assertOutputContainsOsIndependent("Java-style resources: [lib/betaSrc/freeBetaRelease/resources, lib/src/androidFreeBetaRelease/resources]")
            }

            buildAndFail("testFreeBetaDebug") {
                assertOutputContains("CommonTest > fail FAILED")
                assertOutputContains("TestKotlin > fail FAILED")
                assertOutputContains("AndroidTestKotlin > fail FAILED")
                assertOutputContains("TestJava > fail FAILED")
            }

            build("assemble")

            buildAndFail("connectedAndroidTest") {
                assertOutputContains("No connected devices!")
            }
        }
    }

    @DisplayName("KT-27170: android lint works with dependency on non-android mpp project")
    @GradleAndroidTest
    fun testLintInAndroidProjectsDependingOnMppWithoutAndroid(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        disabledOnWindowsWhenAgpVersionIsLowerThan(agpVersion, "7.4.0", "Lint leaves opened file descriptors")
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(androidVersion = agpVersion)
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
            buildJdk = jdkVersion.location
        ) {
            includeOtherProjectAsSubmodule(pathPrefix = "new-mpp-lib-and-app", otherProjectName = "sample-lib")
            subProject("Lib").buildGradle.appendText(
                //language=Gradle
                """

                dependencies { implementation(project(':sample-lib')) }
                """.trimIndent()
            )
            val lintTask = ":Lib:lintFlavor1Debug"
            build(lintTask) {
                assertTasksExecuted(lintTask) // Check that the lint task ran successfully, KT-27170
            }
        }
    }

    @DisplayName("MPP allTests task depending on Android unit tests")
    @GradleAndroidTest
    fun testMppAllTests(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(androidVersion = agpVersion)
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
            buildJdk = jdkVersion.location
        ) {
            build(":lib:allTests", "--dry-run") {
                assertOutputContains(":lib:testDebugUnitTest SKIPPED")
                assertOutputContains(":lib:testReleaseUnitTest SKIPPED")
            }
        }
    }

    // https://youtrack.jetbrains.com/issue/KT-48436
    @GradleAndroidTest
    fun testUnusedSourceSetsReportAndroid(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android", gradleVersion,
            defaultBuildOptions
                .copy(androidVersion = agpVersion)
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                output.assertNoDiagnostic(KotlinToolingDiagnostics.UnusedSourceSetsWarning)
            }
        }
    }
}
