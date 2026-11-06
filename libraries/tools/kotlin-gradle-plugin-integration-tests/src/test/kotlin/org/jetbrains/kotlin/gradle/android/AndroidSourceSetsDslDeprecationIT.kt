/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@AndroidGradlePluginTests
@DisplayName("sourceSets DSL in Kotlin Android plugin should properly report deprecation diagnostics")
class AndroidSourceSetsDslDeprecationIT : KGPBaseTest() {

    @DisplayName("KT-91331: sourceSets with Groovy closure syntax using getByName should report a diagnostic")
    @TestMetadata("kt-91331-androidSourceSetsGroovyDslGetByName")
    @GradleAndroidTest
    fun `test sourceSets with Groovy closure syntax using getByName should not crash`(
        gradleVersion: GradleVersion,
        agpVersion: String,
    ) {
        project(
            "kt-91331-androidSourceSetsGroovyDslGetByName",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ) {
            build("tasks") {
                assertHasDiagnostic(KotlinToolingDiagnostics.SourceSetsAccessInAndroidExtension)
            }
        }
    }

    @DisplayName("KT-91331: sourceSets with Kotlin DSL getByName should report a diagnostic")
    @TestMetadata("kt-91331-androidSourceSetsKotlinDslGetByName")
    @GradleAndroidTest
    fun `test sourceSets with Kotlin DSL getByName should not crash`(
        gradleVersion: GradleVersion,
        agpVersion: String,
    ) {
        project(
            "kt-91331-androidSourceSetsKotlinDslGetByName",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ) {
            build("tasks") {
                assertHasDiagnostic(KotlinToolingDiagnostics.SourceSetsAccessInAndroidExtension)
            }
        }
    }
}
