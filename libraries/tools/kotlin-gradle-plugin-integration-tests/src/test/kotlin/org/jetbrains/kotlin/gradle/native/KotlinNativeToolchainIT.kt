/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("This test class contains base scenarios for testing Kotlin Native toolchain feature")
@NativeGradlePluginTests
class KotlinNativeToolchainIT : KGPBaseTest() {

    @DisplayName(
        "KT-66750: check that disabled native toolchain flag in subproject does not affect root project"
    )
    @GradleTest
    fun checkCommonizeNativeDistributionWithPlatform(gradleVersion: GradleVersion) {
        nativeProject("commonize-kt-66750-with-subproject", gradleVersion) {
            // commonizeNativeDistribution is added only when isolated projects support mode is disabled
            // so Gradle isolated projects should be also disabled
            val buildOptions = defaultBuildOptions.disableKmpIsolatedProjectSupport().disableIsolatedProjects()
            build(":commonizeNativeDistribution", buildOptions = buildOptions)
        }
    }

}
