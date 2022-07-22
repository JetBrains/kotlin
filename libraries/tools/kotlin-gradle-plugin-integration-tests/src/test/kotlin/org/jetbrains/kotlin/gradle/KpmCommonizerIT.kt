/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.internal.os.OperatingSystem
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.allFilesWithExtension
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.fail

@MppGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.MIN_SUPPORTED_KPM)
class KpmCommonizerIT : KGPBaseTest() {

    @GradleTest
    fun `test commonizeNativeDistributionWithIosLinuxWindows`(gradleVersion: GradleVersion) {
        project(
            projectName = "commonizeNativeDistributionWithIosLinuxWindows",
            gradleVersion = gradleVersion
        ) {
            swapKpmScripts("build.gradle.kts")

            build("cleanNativeDistributionCommonization")
            build("commonize", "-Pkotlin.mpp.enableNativeDistributionCommonizationCache=false") {
                assertTasksExecuted(":commonizeNativeDistribution")
                assertOutputContains("Preparing commonized Kotlin/Native libraries")
            }
            build("commonize", "-Pkotlin.mpp.enableNativeDistributionCommonizationCache=true") {
                assertTasksUpToDate(":commonizeNativeDistribution")
                assertOutputContains("Native Distribution Commonization: Cache hit")
                assertOutputContains("Native Distribution Commonization: All available targets are commonized already")
                assertOutputContains("Native Distribution Commonization: Lock acquired")
                assertOutputContains("Native Distribution Commonization: Lock released")
                assertOutputDoesNotContain("Preparing commonized Kotlin/Native libraries")
            }
            build("commonize", "-Pkotlin.mpp.enableNativeDistributionCommonizationCache=false") {
                assertTasksExecuted(":commonizeNativeDistribution")
                assertOutputContains("Native Distribution Commonization: Cache disabled")
                assertOutputContains("Preparing commonized Kotlin/Native libraries")
            }
        }
    }

    @GradleTest
    fun `check commonized posix klib was used in KN metadata compilation`(gradleVersion: GradleVersion) {
        project(
            projectName = "kpm-commonizer",
            gradleVersion = gradleVersion
        ) {
            prepareTargets()

            build("cleanNativeDistributionCommonization")
            build("compileNativeMainKotlinNativeMetadata") {
                assertTasksExecuted(":commonizeNativeDistribution")
                /*
                Regex finds lines like this:
                    -l
                    "/.konan/kotlin-native-prebuilt-macos-x86_64-1.8.0-dev-287/klib/commonized/1.8.255-SNAPSHOT/(linux_x64, macos_x64)/org.jetbrains.kotlin.native.platform.posix"
                */
                assertOutputContains(Regex("""-l\n\s".*\.konan.*klib.*commonized.*/org\.jetbrains\.kotlin\.native\.platform\.posix""""))
            }
        }
    }

    private fun TestProject.swapKpmScripts(fileName: String) {
        val dir = projectPath.toFile()
        val old = dir.walk().filter { it.isFile && it.name.equals(fileName, ignoreCase = true) }
        old.forEach { file ->
            val kpmFile = file.parentFile.resolve("$fileName-kpm")
            if (kpmFile.exists()) {
                file.delete()
                kpmFile.renameTo(file)
            }
        }
    }

    private fun TestProject.prepareTargets() {
        val dir = projectPath.toFile()
        val scripts = dir.walk().filter { it.isFile && it.name.equals("build.gradle.kts", ignoreCase = true) }
        scripts.forEach { script ->
            val originalText = script.readText()
            val preparedText = originalText
                .replace("<targetA>", CommonizableTargets.targetA.simpleName)
                .replace("<targetB>", CommonizableTargets.targetB.simpleName)
            script.writeText(preparedText)
        }
    }

    private object CommonizableTargets {
        private val os = OperatingSystem.current()

        val targetA = when {
            os.isMacOsX -> GradleKpmMacosX64Variant::class.java
            os.isLinux -> GradleKpmLinuxX64Variant::class.java
            os.isWindows -> GradleKpmMingwX64Variant::class.java
            else -> fail("Unsupported os: ${os.name}")
        }

        val targetB = when {
            os.isMacOsX -> GradleKpmLinuxX64Variant::class.java
            os.isLinux -> GradleKpmLinuxArm64Variant::class.java
            os.isWindows -> GradleKpmMingwX86Variant::class.java
            else -> fail("Unsupported os: ${os.name}")
        }
    }
}