/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.declarative

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemPlugin
import org.jetbrains.kotlin.gradle.ecosystem.internal.declarative.KotlinDeclarativePlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.writeText

@DisplayName("DCL plugin application")
class DeclarativeGradleApplyIT : KGPBaseTest() {

    @MppGradlePluginTests
    @GradleTestVersions(
        minVersion = DCL_SUPPORTED_GRADLE_VERSION,
        maxVersion = DCL_SUPPORTED_GRADLE_VERSION,
    )
    @GradleTest
    fun shouldNotApplyByDefaultInSuitableGradleVersion(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            addEcosystemPluginToBuildScriptCompilationClasspath()

            settingsBuildScriptInjection {
                settings.plugins.apply("org.jetbrains.kotlin.ecosystem")
            }

            build("help") {
                assertOutputContains(KotlinEcosystemPlugin.EXPERIMENTAL_WARNING_MESSAGE)
                assertOutputContains(KotlinEcosystemPlugin.DCL_DISABLED_MESSAGE)
            }
        }
    }

    @MppGradlePluginTests
    @GradleTestVersions(
        minVersion = DCL_SUPPORTED_GRADLE_VERSION,
        maxVersion = DCL_SUPPORTED_GRADLE_VERSION,
    )
    @GradleTest
    fun shouldEnabledDclSupportInSuitableGradleVersion(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            addEcosystemPluginToBuildScriptCompilationClasspath()

            gradleProperties.writeText(
                """
                kotlin.dclEnabled=true
                """.trimIndent()
            )
            settingsBuildScriptInjection {
                settings.plugins.apply("org.jetbrains.kotlin.ecosystem")
            }

            build("help") {
                assertOutputContains(KotlinEcosystemPlugin.EXPERIMENTAL_WARNING_MESSAGE)
                assertOutputContains(KotlinDeclarativePlugin.DCL_ENABLED_MESSAGE)
            }
        }
    }

    @MppGradlePluginTests
    @GradleTestVersions(
        minVersion = DCL_UNSUPPORTED_GRADLE_VERSION,
        maxVersion = DCL_UNSUPPORTED_GRADLE_VERSION,
    )
    @GradleTest
    fun shouldDisableDclSupportInNonSuitableGradleVersion(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            addEcosystemPluginToBuildScriptCompilationClasspath()

            gradleProperties.writeText(
                """
                kotlin.dclEnabled=true
                """.trimIndent()
            )
            settingsBuildScriptInjection {
                settings.plugins.apply("org.jetbrains.kotlin.ecosystem")
            }

            build("help") {
                assertOutputContains(KotlinEcosystemPlugin.EXPERIMENTAL_WARNING_MESSAGE)
                assertOutputContains(KotlinEcosystemPlugin.buildDclUnsupportedMessage(gradleVersion))
            }
        }
    }

    companion object {
        // Defined in 'KotlinDeclarativePlugin.SUPPORTED_GRADLE_VERSION'
        private const val DCL_SUPPORTED_GRADLE_VERSION = TestVersions.Gradle.G_8_14
        private const val DCL_UNSUPPORTED_GRADLE_VERSION = TestVersions.Gradle.G_8_13
    }
}