/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Tests for K/N with enabled configuration cache")
@NativeGradlePluginTests
class NativeWithConfigurationCacheIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copy(configurationCache = true)

    @DisplayName(
        "Configuration phase should be reused from configuration cache for the second build " +
                "(downloading konan does not affect cache inputs)"
    )
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_1) // Since 8.1 Gradle on configuration cache it detects when the build logic accesses the "outside world" more strict https://docs.gradle.org/8.1.1/release-notes.html#configuration-inputs-detection-improvements
    @GradleTest
    fun testConfigurationCacheReusedSecondTime(gradleVersion: GradleVersion) {
        nativeProject("native-with-configuration-cache", gradleVersion, enableGradleDebug = true) {
            // we need to download compiler on the first build, that is why we are setting custom konan home dir without any compiler inside
            val localKonan = workingDir.resolve(".konan")
            build("help", "-Pkonan.data.dir=$localKonan") {
                assertOutputContains("Configure project")
                assertOutputContains("Unpack Kotlin/Native compiler to")
            }

            build("help", "-Pkonan.data.dir=$localKonan") {
                assertOutputContains("Reusing configuration cache.")
            }
        }
    }
}