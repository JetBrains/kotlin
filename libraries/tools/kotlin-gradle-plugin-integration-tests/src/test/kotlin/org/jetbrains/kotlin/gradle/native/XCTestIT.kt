/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceFirst
import org.jetbrains.kotlin.konan.target.Xcode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Integration of the Kotlin/Native XCTest support in tests")
@NativeGradlePluginTests
internal class XCTestIT : KGPBaseTest() {

    private val devFrameworkPath by lazy { "${Xcode.findCurrent().iphonesimulatorSdkPlatform}/Developer/Library/Frameworks/" }

    @DisplayName("A simple test bundle producer")
    @GradleTest
    fun shouldProduceTestBundle(gradleVersion: GradleVersion) {
        nativeProject("native-apple-test-bundle", gradleVersion) {
            buildGradleKts.replaceFirst("<PATH_TO_FRAMEWORKS>", devFrameworkPath)
            build("linkSimpleTestDebugTestBundleIosSimulatorArm64", forceOutput = true) {
                assertDirectoryInProjectExists("build/bin/iosSimulatorArm64/simpleTestDebugTestBundle/simpleTest.xctest")
            }
        }
    }
}