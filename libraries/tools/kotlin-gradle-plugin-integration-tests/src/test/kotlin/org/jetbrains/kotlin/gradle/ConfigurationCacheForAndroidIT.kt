/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Before
import org.junit.Test

class ConfigurationCacheForAndroidIT : AbstractConfigurationCacheIT() {
    private val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v4_2_0

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.AtLeast("6.7.1")

    override fun defaultBuildOptions() =
        super.defaultBuildOptions().copy(
            androidHome = KtTestUtil.findAndroidSdk(),
            androidGradlePluginVersion = androidGradlePluginVersion,
            configurationCache = true,
            configurationCacheProblems = ConfigurationCacheProblems.FAIL
        )

    @Test
    fun testAndroidKaptProject() = with(Project("android-dagger", directoryPrefix = "kapt2")) {
        setupWorkingDir()
        projectDir.resolve("gradle.properties").appendText("\nkapt.incremental.apt=false")
        testConfigurationCacheOf(":app:compileDebugKotlin", ":app:kaptDebugKotlin", ":app:kaptGenerateStubsDebugKotlin")
    }

    @Test
    fun testKotlinAndroidProject() = with(Project("AndroidProject")) {
        setupWorkingDir()
        testConfigurationCacheOf(":Lib:compileFlavor1DebugKotlin", ":Android:compileFlavor1DebugKotlin")
    }

    @Test
    fun testKotlinAndroidProjectTests() = with(Project("AndroidIncrementalMultiModule")) {
        setupWorkingDir()
        testConfigurationCacheOf(
            ":app:compileDebugAndroidTestKotlin", ":app:compileDebugUnitTestKotlin",
            buildOptions = defaultBuildOptions().copy(warningMode = WarningMode.Summary)
        )
    }
}
