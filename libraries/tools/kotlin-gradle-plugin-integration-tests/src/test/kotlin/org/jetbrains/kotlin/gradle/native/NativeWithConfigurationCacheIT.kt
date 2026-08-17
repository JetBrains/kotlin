/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.BrokenOnMacosTest
import org.jetbrains.kotlin.gradle.BrokenOnMacosTestFailureExpectation
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Tests for K/N with enabled configuration cache")
@NativeGradlePluginTests
class NativeWithConfigurationCacheIT : KGPBaseTest() {

    @DisplayName(
        "Configuration phase should be reused from configuration cache for the second build " +
                "(downloading konan does not affect cache inputs)"
    )
    @GradleTest
    @BrokenOnMacosTest(failureExpectation = BrokenOnMacosTestFailureExpectation.ALWAYS)
    fun testConfigurationCacheReusedSecondTime(gradleVersion: GradleVersion) {
        nativeProject(
            "native-with-configuration-cache",
            gradleVersion,
            dependencyManagement = DependencyManagement.DisabledDependencyManagement,
            buildOptions = defaultBuildOptions.copy(
                // We need to download compiler on the first build, that is why we are setting custom konan home dir without any compiler inside
                konanDataDir = workingDir.resolve(".konan"),
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    version = "2.1.21", // an arbitrary release version
                    distributionDownloadFromMaven = false, // please, remove the whole test, when this flag will be removed
                ),
            ),
        ) {
            build(
                "help"
            ) {
                assertOutputContains("Configure project")
                assertOutputContains("Unpack Kotlin/Native compiler to")
            }

            build("help") {
                assertOutputContains("Reusing configuration cache.")
            }
        }
    }
}
