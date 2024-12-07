/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceWithVersion
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("One-off incremental scenarios with KMP - K2")
class KmpIncrementalCompilationMiscIT : KGPBaseTest() {

    /**
     * Enable debug logs and search for `[DEBUG] [TestEventLogger]` in test outputs to investigate
     */

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            enableUnsafeIncrementalCompilationForMultiplatform = true,
        )

    @Disabled("Broken, see KT-59153")
    @DisplayName("KT-59153 - incremental build when interface changes - intra-module version - jvm")
    @GradleTest
    @TestMetadata("kt-59153-default-impl-interface-in-kmp")
    fun testInModuleChangeJvm(gradleVersion: GradleVersion) {
        project(
            "kt-59153-default-impl-interface-in-kmp",
            gradleVersion
        ) {
            build("app:jvmTest") {
                assertTestResults(
                    projectPath.resolve("TEST-jvm.xml"),
                    "jvmTest",
                    subprojectName = "app"
                )
            }

            projectPath.resolve("app/src/commonTest/kotlin/AppInterface.kt").replaceWithVersion("withExplicitSignature")

            build("app:jvmTest") {
                assertTestResults(
                    projectPath.resolve("TEST-jvm.xml"),
                    "jvmTest",
                    subprojectName = "app"
                )
            }
        }
    }

    @DisplayName("KT-59153 - incremental build when interface changes - intra-module version - js")
    @GradleTest
    @TestMetadata("kt-59153-default-impl-interface-in-kmp")
    fun testInModuleChangeJs(gradleVersion: GradleVersion) {
        project(
            "kt-59153-default-impl-interface-in-kmp",
            gradleVersion
        ) {
            build("app:jsNodeTest") {
                assertTestResults(
                    projectPath.resolve("TEST-js.xml"),
                    "jsNodeTest",
                    subprojectName = "app"
                )
            }

            projectPath.resolve("app/src/commonTest/kotlin/AppInterface.kt").replaceWithVersion("withExplicitSignature")

            build("app:jsNodeTest") {
                assertTestResults(
                    projectPath.resolve("TEST-js.xml"),
                    "jsNodeTest",
                    subprojectName = "app"
                )
            }
        }
    }

    @Disabled("Broken, see KT-59153")
    @DisplayName("KT-59153 - incremental build when interface changes - cross module version - jvm")
    @GradleTest
    @TestMetadata("kt-59153-default-impl-interface-in-kmp")
    fun testCrossModuleChangeJvm(gradleVersion: GradleVersion) {
        project(
            "kt-59153-default-impl-interface-in-kmp",
            gradleVersion
        ) {
            build("app:jvmTest") {
                assertTestResults(
                    projectPath.resolve("TEST-jvm.xml"),
                    "jvmTest",
                    subprojectName = "app"
                )
            }

            projectPath.resolve("lib/src/commonMain/kotlin/LibInterface.kt")
                .replaceWithVersion("withExplicitSignature")

            build("app:jvmTest") {
                assertTestResults(
                    projectPath.resolve("TEST-jvm.xml"),
                    "jvmTest",
                    subprojectName = "app"
                )
            }
        }
    }

    @DisplayName("KT-59153 - incremental build when interface changes - cross module version - js")
    @GradleTest
    @TestMetadata("kt-59153-default-impl-interface-in-kmp")
    fun testCrossModuleChangeJs(gradleVersion: GradleVersion) {
        project(
            "kt-59153-default-impl-interface-in-kmp",
            gradleVersion
        ) {
            build("app:jsNodeTest") {
                assertTestResults(
                    projectPath.resolve("TEST-js.xml"),
                    "jsNodeTest",
                    subprojectName = "app"
                )
            }

            projectPath.resolve("lib/src/commonMain/kotlin/LibInterface.kt")
                .replaceWithVersion("withExplicitSignature")

            build("app:jsNodeTest") {
                assertTestResults(
                    projectPath.resolve("TEST-js.xml"),
                    "jsNodeTest",
                    subprojectName = "app"
                )
            }
        }
    }
}
