/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests

@SwiftPMImportGradlePluginTests
class TestSwiftPMImportGradlePluginTag : KGPBaseTest() {
    @GradleTest
    fun test(version: GradleVersion) {
        // Noop test: remove after SwiftPM import feature merge into master
    }
}