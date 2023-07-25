/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

// This test is a JS test
// but, as it is the only one to required to run on MacOS
// we are running this test as a part of Native tests
// This allows us to keep CI configuration simpler.
@OsCondition(supportedOn = [OS.LINUX, OS.MAC, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.MAC, OS.WINDOWS])
@NativeGradlePluginTests
class JsSetupConfigurationCacheIT : KGPBaseTest() {

    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(
            configurationCache = true,
            configurationCacheProblems = BaseGradleIT.ConfigurationCacheProblems.FAIL
        )

    // hack to be run on Mac m*
    @DisplayName("Check Node.JS setup on different platforms")
    @GradleTest
    fun checkNodeJsSetup(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            build("kotlinUpgradeYarnLock") {
                assertTasksExecuted(":kotlinUpgradeYarnLock")
                assertConfigurationCacheStored()
            }

            build("kotlinUpgradeYarnLock") {
                assertTasksUpToDate(":kotlinUpgradeYarnLock")
                assertConfigurationCacheReused()
            }
        }
    }
}