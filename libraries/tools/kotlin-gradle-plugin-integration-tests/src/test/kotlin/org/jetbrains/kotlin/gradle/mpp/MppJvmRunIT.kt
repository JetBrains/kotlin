/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import kotlin.io.path.appendText

@MppGradlePluginTests
class MppJvmRunIT : KGPBaseTest() {

    @GradleTest
    fun `test jvmRun`(version: GradleVersion) {
        project("mppRunJvm", version) {
            build("jvmRun", "-PmainClass=JvmMainKt") {
                assertOutputContains("Jvm: OK!")
                assertOutputContains("Executed: JvmMain")
            }

            build("jvmRun", "-PmainClass=CommonMainKt") {
                assertOutputContains("Jvm: OK!")
                assertOutputContains("Executed: CommonMain")
            }

            // No mainClass provided!
            buildAndFail("jvmRun")

            /* Provided mainClass in buildscript */
            projectPath.resolve("multiplatform").resolve("build.gradle.kts").appendText(
                """
                kotlin {
                    jvm().mainRun {
                        mainClass.set("JvmMainKt")
                    }
                }
                """.trimIndent()
            )

            build("jvmRun") {
                assertOutputContains("Jvm: OK!")
                assertOutputContains("Executed: JvmMain")
            }

            /* Overwrite buildscript mainClass via property */
            build("jvmRun", "-PmainClass=CommonMainKt") {
                assertOutputContains("Jvm: OK!")
                assertOutputContains("Executed: CommonMain")
            }
        }
    }

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_4)
    @GradleTest
    fun `test - jvmRun - works with Gradle configuration cache`(version: GradleVersion) {
        project("mppRunJvm", version, buildOptions = defaultBuildOptions.copy(configurationCache = true)) {
            build("jvmRun", "-DmainClass=JvmMainKt") {
                assertOutputContains("Jvm: OK!")
                assertOutputContains("Executed: JvmMain")
                assertConfigurationCacheStored()
            }

            build("jvmRun", "-DmainClass=CommonMainKt") {
                assertOutputContains("Jvm: OK!")
                assertOutputContains("Executed: CommonMain")
                assertConfigurationCacheReused()
            }

            build("jvmRun", "-DmainClass=JvmMainKt") {
                assertOutputContains("Jvm: OK!")
                assertOutputContains("Executed: JvmMain")
                assertConfigurationCacheReused()
            }
        }
    }
}
