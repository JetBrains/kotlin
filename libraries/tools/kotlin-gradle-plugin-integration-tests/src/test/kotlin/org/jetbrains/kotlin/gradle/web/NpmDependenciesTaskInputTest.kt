/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.web

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.JsGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.project


@JsGradlePluginTests
class NpmDependenciesTaskInputTest : KGPBaseTest() {
    @GradleTest
    fun v1(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion = gradleVersion) {

        }
    }
}
