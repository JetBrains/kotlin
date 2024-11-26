/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.util.replaceWithVersion
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Specific incremental scenarios with local classes in KMP - K2")
class KmpIncrementalCompilationWithLocalClassesIT : KGPBaseTest() {

    /**
     * Note for Disabled KT-59153 tests -
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

@MppGradlePluginTests
@DisplayName("Scenarios with multi-step incremental compilation in KMP - K2")
class KmpIncrementalCompilationSetExpansionIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            // it's more convenient to set up the test project using common sourceset
            enableUnsafeIncrementalCompilationForMultiplatform = true,
        )

    @DisplayName("Inline cycle with monotonous expansion")
    @GradleTest
    @TestMetadata("kt-29860-basic-inline-loop-kmp")
    fun testInlineCycleWithMonotonousExpansion(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(enableMonotonousIncrementalCompileSetExpansion = true)

        project("kt-29860-basic-inline-loop-kmp", gradleVersion) {
            build("compileKotlinJvm", "compileKotlinJs")

            val aKt = projectPath.resolve("src/commonMain/kotlin/a.kt")
            val bKt = projectPath.resolve("src/commonMain/kotlin/b.kt")

            projectPath.resolve("src/commonMain/kotlin/b.kt").replaceWithVersion("justChange")
            build("compileKotlinJvm", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertKotlinCompilationSteps(
                    listOf(bKt).relativizeTo(projectPath),
                    listOf(aKt, bKt).relativizeTo(projectPath)
                )
            }
            build("compileKotlinJs", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertKotlinCompilationSteps(
                    /* js(ir) instantly detects that a.kt is affected - good for it */
                    listOf(bKt, aKt).relativizeTo(projectPath)
                )
            }
        }
    }

    @DisplayName("Inline cycle without monotonous expansion")
    @GradleTest
    @TestMetadata("kt-29860-basic-inline-loop-kmp")
    fun testInlineCycleWithoutMonotonousExpansion(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(enableMonotonousIncrementalCompileSetExpansion = false)

        project("kt-29860-basic-inline-loop-kmp", gradleVersion) {
            build("compileKotlinJvm", "compileKotlinJs")

            val aKt = projectPath.resolve("src/commonMain/kotlin/a.kt")
            val bKt = projectPath.resolve("src/commonMain/kotlin/b.kt")

            bKt.replaceWithVersion("justChange")
            build("compileKotlinJvm", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertKotlinCompilationSteps(
                    listOf(bKt).relativizeTo(projectPath),
                    listOf(aKt).relativizeTo(projectPath)
                )
            }
            build("compileKotlinJs", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertKotlinCompilationSteps(
                    /* js(ir) instantly detects that a.kt is affected - good for it */
                    listOf(bKt, aKt).relativizeTo(projectPath)
                )
            }
        }
    }

    @DisplayName("Inline cycle with monotonous expansion - avoiding infinite loop")
    @GradleTest
    @TestMetadata("kt-29860-basic-inline-loop-kmp")
    fun testInlineCycleWithMonotonousExpansionAvoidingInfiniteLoop(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(enableMonotonousIncrementalCompileSetExpansion = true)

        project("kt-29860-basic-inline-loop-kmp", gradleVersion) {
            // js variant of this scenario is different per KT-29860 - compiler backend handles the loop detection
            build("compileKotlinJvm")

            val aKt = projectPath.resolve("src/commonMain/kotlin/a.kt")
            val bKt = projectPath.resolve("src/commonMain/kotlin/b.kt")

            bKt.replaceWithVersion("addCircularDependencyOnA")
            buildAndFail("compileKotlinJvm", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertKotlinCompilationSteps(
                    listOf(bKt).relativizeTo(projectPath),
                    listOf(aKt, bKt).relativizeTo(projectPath)
                )
                assertOutputContains("Type checking has run into a recursive problem.")
            }
        }
    }
}
