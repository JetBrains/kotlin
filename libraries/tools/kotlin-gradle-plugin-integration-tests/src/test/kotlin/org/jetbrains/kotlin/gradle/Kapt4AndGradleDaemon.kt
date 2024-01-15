/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.DaemonsGradlePluginTests
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName

@DisplayName("Kapt4 caching inside Gradle daemon")
@DaemonsGradlePluginTests
class Kapt4AndGradleDaemon : Kapt3AndGradleDaemon() {
    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copyEnsuringK2()

    @Disabled("KT-63102 Incremental compilation doesn't work in 2.0")
    override fun testAnnotationProcessorClassIsLoadedOnce(gradleVersion: GradleVersion) {}
}