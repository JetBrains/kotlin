/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Paths
import kotlin.io.path.appendText
import kotlin.io.path.deleteRecursively

@OsCondition(
    // This test is disabled for Windows, because there is an issue with
    // KotlinToolRunner.getIsolatedClassLoader does not release konan jars properly. See KT-62093 for more details.
    supportedOn = [OS.LINUX, OS.MAC],
    enabledOnCI = [OS.LINUX, OS.MAC]
)
class NativeDownloadAndPlatformLibsNonParallelIT : KGPDaemonsBaseTest() {

    private val platformName: String = HostManager.platformName()
    private val currentCompilerVersion = NativeCompilerDownloader.DEFAULT_KONAN_VERSION

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.withBundledKotlinNative().copy()


    @DisplayName("Downloading K/N distribution in default .konan dir")
    @GradleTest
    fun testLibrariesGenerationInDefaultKonanDir(gradleVersion: GradleVersion) {

        checkThatDefaultKonanHasNotBeenCreated()

        val userHomeDir = System.getProperty("user.home")
        platformLibrariesProject("linuxX64", gradleVersion = gradleVersion) {
            build(
                "assemble",
                buildOptions = defaultBuildOptions.copy(konanDataDir = null) // we need to download konan bundle to default dir
            ) {
                assertOutputContains("Moved Kotlin/Native bundle from .* to $userHomeDir/.konan/kotlin-native-prebuilt-$platformName-$currentCompilerVersion".toRegex())
                assertOutputDoesNotContain("Generate platform libraries for ")

                // checking that konan was downloaded and native dependencies were not downloaded into ~/.konan dir
                assertDirectoryExists(Paths.get("$userHomeDir/.konan/dependencies"))
                assertDirectoryExists(Paths.get("$userHomeDir/.konan/kotlin-native-prebuilt-$platformName-$currentCompilerVersion"))
            }
        }

        // clean ~/.konan after test it should not be with all inheritors of KGPBaseTest
        Paths.get("$userHomeDir/.konan").deleteRecursively()

    }

    private fun platformLibrariesProject(
        vararg targets: String,
        gradleVersion: GradleVersion,
        test: TestProject.() -> Unit = {},
    ) {
        nativeProject("native-platform-libraries", gradleVersion) {
            buildGradleKts.appendText(
                targets.joinToString(prefix = "\n", separator = "\n") {
                    "kotlin.$it()"
                }
            )
            test()
        }
    }
}