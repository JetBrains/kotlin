/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.DeprecatedWarningGradleProperties
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.PluginLoadedInMultipleProjectsError
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Different Gradle classloaders warning")
@MppGradlePluginTests
class DifferentClassloadersIT : KGPBaseTest() {

    @DisplayName("Different classloaders error is not thrown")
    @GradleTest
    fun testDifferentClassloadersNotThrown(gradleVersion: GradleVersion) {
        project(
            "differentClassloaders",
            gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            build("publish", "-PmppProjectDependency=true") {
                assertNoDiagnostic(PluginLoadedInMultipleProjectsError)
            }
        }
    }

    @DisplayName("Different classloader error is thrown on different plugin versions")
    @GradleTest
    fun testDetectingDifferentClassLoadersError(gradleVersion: GradleVersion) {
        project(
            "differentClassloaders",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            setupDifferentClassloadersProject()

            // after enabling isolated projects support by default we should not fail the build
            buildAndFail("publish", "-PmppProjectDependency=true") {
                assertHasDiagnostic(PluginLoadedInMultipleProjectsError)
            }
        }
    }

    @DisplayName("KT-50598: Different classloaders error can be disabled")
    @GradleTest
    fun differentClassloadersErrorCanBeDisabled(gradleVersion: GradleVersion) {
        project(
            "differentClassloaders",
            gradleVersion,
            // CC should be explicitly disabled because it hides the warning on subsequent builds
            buildOptions = defaultBuildOptions.copy(configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED),
        ) {
            setupDifferentClassloadersProject()

            fun checkThatErrorIsThrown() {
                build("-PmppProjectDependency=true") {
                    assertHasDiagnostic(
                        PluginLoadedInMultipleProjectsError,
                        withSubstring = "':jvm-app', ':mpp-lib'"
                    )
                }
            }

            checkThatErrorIsThrown()

            // check that the error is also thrown on subsequent builds
            checkThatErrorIsThrown()

            // Test the flag that turns off the warnings
            build("-PmppProjectDependency=true", "-Pkotlin.pluginLoadedInMultipleProjects.ignore=true") {
                assertNoDiagnostic(PluginLoadedInMultipleProjectsError)
                assertHasDiagnostic(
                    DeprecatedWarningGradleProperties,
                    withSubstring = "kotlin.pluginLoadedInMultipleProjects.ignore",
                )
            }
        }
    }

    private fun TestProject.setupDifferentClassloadersProject() {
        // Specify the plugin versions in the subprojects with different plugin sets –
        // this will make Gradle use separate class loaders
        buildGradle.modify {
            it.checkedReplace("id \"org.jetbrains.kotlin.multiplatform\"", "//")
        }
        subProject("mpp-lib").buildGradle.modify {
            it.checkedReplace(
                "id \"org.jetbrains.kotlin.multiplatform\"",
                "id \"org.jetbrains.kotlin.multiplatform\" version \"${TestVersions.Kotlin.CURRENT}\""
            )
        }
        subProject("jvm-app").buildGradle.modify {
            it.checkedReplace(
                "id \"org.jetbrains.kotlin.jvm\"",
                "id \"org.jetbrains.kotlin.jvm\" version \"${TestVersions.Kotlin.CURRENT}\""
            )
        }

        // Also include another project via a composite build:
        includeOtherProjectAsIncludedBuild("allopenPluginsDsl", "pluginsDsl")
        buildGradle.appendText(
            "\ntasks.create(\"publish\").dependsOn(gradle.includedBuild(\"allopenPluginsDsl\").task(\":assemble\"))"
        )
    }
}
