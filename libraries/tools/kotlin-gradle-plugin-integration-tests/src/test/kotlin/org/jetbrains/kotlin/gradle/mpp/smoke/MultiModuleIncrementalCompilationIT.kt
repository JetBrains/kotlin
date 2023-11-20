/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.smoke

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName

@Disabled("KT-56963")
@DisplayName("Basic multi-module incremental scenarios with KMP - K2")
@MppGradlePluginTests
open class MultiModuleIncrementalCompilationIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK2()

    @DisplayName("Verify IC builds on change in lib/commonMain")
    @GradleTest
    fun testTouchLibCommon(gradleVersion: GradleVersion) {
        /**
         * [Cross-Module] touch libCommon, affect appCommon, appJs
         * Variants: 1. code-compatible ABI breakage (change deduced return type), 2. no ABI breakage, 3. error + fix
         */

    }

    @DisplayName("Verify IC builds on change in lib/platformMain")
    @GradleTest
    fun testTouchLibPlatform(gradleVersion: GradleVersion) {
        /**
         * [C-M] touch lbJs, affect appJs
         * Variants: 1. code-compatible ABI breakage (change deduced return type), 2. no ABI breakage, 3. error + fix
         */

    }

    @DisplayName("Verify IC builds on change in app/commonMain")
    @GradleTest
    fun testTouchAppCommon(gradleVersion: GradleVersion) {
        /**
         * [C-M] touch appCommon, rebuild appJs
         * Variants: 1. code-compatible ABI breakage (change deduced return type), 2. no ABI breakage, 3. error + fix
         */

        //TODO KT-56963 : confirm and create issues for these source-compatible changes
        //utilKtPath.replaceFirst("fun multiplyByTwo(n: Int): Int", "fun <T> multiplyByTwo(n: T): T") - breaks native
        //utilKtPath.replaceFirst("fun multiplyByTwo(n: Int): Int", "fun multiplyByTwo(n: Int, unused: Int = 42): Int") - breaks js
    }

    @DisplayName("Verify IC builds on change in app/platformMain")
    @GradleTest
    fun testTouchAppPlatform(gradleVersion: GradleVersion) {
        /**
         * [C-M] touch appJs, rebuild appJs
         * Variants: 1. code-compatible ABI breakage (change deduced return type), 2. no ABI breakage, 3. error + fix
         */

    }
}

@DisplayName("Incremental scenarios with expect/actual - K1")
class MultiModuleIncrementalCompilationK1IT : MultiModuleIncrementalCompilationIT() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK1()
}
