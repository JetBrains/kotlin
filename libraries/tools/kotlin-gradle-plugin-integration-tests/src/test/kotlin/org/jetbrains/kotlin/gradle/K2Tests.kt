/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import kotlin.test.Ignore

@Disabled("Used for local testing only")
@MppGradlePluginTests
@DisplayName("K2: Hierarchical multiplatform")
class K2HierarchicalMppIT : HierarchicalMppIT() {
    override val defaultBuildOptions: BuildOptions get() = super.defaultBuildOptions.copy(languageVersion = "2.0")
}

@Ignore
class K2KlibBasedMppIT : KlibBasedMppIT() {
    override fun defaultBuildOptions(): BuildOptions = super.defaultBuildOptions().copy(languageVersion = "2.0")
}

@Ignore
class K2NewMultiplatformIT : NewMultiplatformIT() {
    override fun defaultBuildOptions(): BuildOptions = super.defaultBuildOptions().copy(languageVersion = "2.0")
}

@Disabled("Used for local testing only")
class K2CommonizerIT : CommonizerIT() {
    override val defaultBuildOptions: BuildOptions get() = super.defaultBuildOptions.copy(languageVersion = "2.0")
}

@Ignore
class K2CommonizerHierarchicalIT : CommonizerHierarchicalIT() {
    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copy(languageVersion = "2.0")
}

@MppGradlePluginTests
@DisplayName("K2: custom tests")
class CustomK2Tests : KGPBaseTest() {
    @GradleTest
    @DisplayName("Serialization plugin in common source set. KT-56911")
    fun testHmppDependenciesInJsTests(gradleVersion: GradleVersion) {
        project(
            "k2-serialization-plugin-in-common-sourceset",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(languageVersion = "2.0"),
        ) {
            val taskToExecute = ":compileKotlinJs"
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @GradleTest
    @DisplayName("HMPP compilation without JS target. KT-57376, KT-57377, KT-57635, KT-57654")
    fun testHmppCompilationWithoutJsTarget(gradleVersion: GradleVersion) {
        with(project("k2-mpp-without-js", gradleVersion)) {
            val taskToExecute = ":compileIntermediateMainKotlinMetadata"
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @GradleTest
    @DisplayName("HMPP compilation with JS target and old stdlib. KT-59151")
    fun testHmppCompilationWithJsAndOldStdlib(gradleVersion: GradleVersion) {
        with(project("k2-mpp-js-old-stdlib", gradleVersion, buildOptions = defaultBuildOptions.copy(languageVersion = "2.0"))) {
            val taskToExecute = ":compileKotlinJs"
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @GradleTest
    @DisplayName("Native metadata of intermediate with reference to internal in common. KT-58219")
    fun nativeMetadataOfIntermediateWithReferenceToInternalInCommon(gradleVersion: GradleVersion) {
        with(project("k2-native-intermediate-metadata", gradleVersion, buildOptions = defaultBuildOptions.copy(languageVersion = "2.0"))) {
            val taskToExecute = ":compileNativeMainKotlinMetadata"
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @Disabled("disable until kotlin/native dependency is updated to include KT-58145")
    @GradleTest
    @DisplayName("Compiling shared native source with FirFakeOverrideGenerator referencing a common entity. KT-58145")
    fun kt581450MppNativeSharedCrash(gradleVersion: GradleVersion) {
        with(
            project(
                "kt-581450-mpp-native-shared-crash",
                gradleVersion,
                buildOptions = defaultBuildOptions.copy(languageVersion = "2.0")
            )
        ) {
            val taskToExecute = ":compileNativeMainKotlinMetadata"
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @Disabled("disable until kotlin/native dependency is updated to include KT-58444")
    @GradleTest
    @DisplayName("Compiling shared native source with intrinsic initializer from common source set in Native-shared source set. KT-58444")
    fun kt58444NativeSharedConstantIntrinsic(gradleVersion: GradleVersion) {
        with(
            project(
                "kt-58444-native-shared-constant-intrinsic",
                gradleVersion,
                buildOptions = defaultBuildOptions.copy(languageVersion = "2.0"),
            )
        ) {
            val taskToExecute = ":compileNativeMainKotlinMetadata"
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @GradleTest
    @DisplayName("Java binary dependency class contains unresolved annotation argument. KT-60181")
    fun kt60181JavaDependencyAnnotatedWithUnresolved(gradleVersion: GradleVersion) {
        with(
            project(
                "k2-java-dep-unresolved-annotation-argument",
                gradleVersion,
                buildOptions = defaultBuildOptions.copy(languageVersion = "2.0"),
            )
        ) {
            val taskToExecute = ":compileKotlin"
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @GradleTest
    @DisplayName("All source sets have opt-in for annotation defined in platform source set. KT-60755")
    fun kt60755OptInDefinedInPlatform(gradleVersion: GradleVersion) {
        with(
            project(
                "k2-mpp-opt-in-in-platform",
                gradleVersion,
                buildOptions = defaultBuildOptions.copy(languageVersion = "2.0"),
            )
        ) {
            val taskToExecute = ":compileKotlinJvm"
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertOutputDoesNotContain("Opt-in requirement marker foo.bar.MyOptIn is unresolved")
            }
        }
    }

    @GradleTest
    @DisplayName("Common metadata compilation. KT-60943")
    fun kt60943CommonMetadataCompilation(gradleVersion: GradleVersion) {
        project(
            "k2-serialization-plugin-in-common-sourceset",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(languageVersion = "2.0"),
        ) {
            val taskToExecute = ":compileCommonMainKotlinMetadata"
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }
        }
    }
}
