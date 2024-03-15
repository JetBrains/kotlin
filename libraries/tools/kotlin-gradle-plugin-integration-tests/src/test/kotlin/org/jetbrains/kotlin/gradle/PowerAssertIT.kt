/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Power-Assert tests")
class PowerAssertIT : KGPBaseTest() {

    @OtherGradlePluginTests
    @DisplayName("power-assert works")
    @GradleTest
    fun testPowerAssertSimple(gradleVersion: GradleVersion) {
        project("powerAssertSimple", gradleVersion) {
            build("check")
        }
    }
}
