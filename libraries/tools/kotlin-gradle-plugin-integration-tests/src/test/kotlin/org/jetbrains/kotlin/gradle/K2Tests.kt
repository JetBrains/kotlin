/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.test.Ignore

@Disabled("Used for local testing only")
@MppGradlePluginTests
@DisplayName("K2: Hierarchical multiplatform")
class K2HierarchicalMppIT : HierarchicalMppIT() {
    override val defaultBuildOptions: BuildOptions get() = super.defaultBuildOptions.copy(languageVersion = "2.0")
}

@MppGradlePluginTests
@DisplayName("KLibs in K2")
class K2KlibBasedMppIT : KlibBasedMppIT() {
    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copyEnsuringK2()
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
    override val defaultBuildOptions: BuildOptions get() = super.defaultBuildOptions.copyEnsuringK2()

    @GradleTest
    @DisplayName("Serialization plugin in common source set. KT-56911")
    fun testHmppDependenciesInJsTests(gradleVersion: GradleVersion) {
        project(
            "k2-serialization-plugin-in-common-sourceset",
            gradleVersion,
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
        with(project("k2-mpp-js-old-stdlib", gradleVersion)) {
            val taskToExecute = ":compileKotlinJs"
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @GradleTest
    @DisplayName("Native metadata of intermediate with reference to internal in common. KT-58219")
    fun nativeMetadataOfIntermediateWithReferenceToInternalInCommon(gradleVersion: GradleVersion) {
        with(project("k2-native-intermediate-metadata", gradleVersion)) {
            val taskToExecute = ":compileNativeMainKotlinMetadata"
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @Disabled("disable until kotlin/native dependency is updated to include KT-61461")
    @GradleTest
    @DisplayName("Native metadata of intermediate with multiple targets. KT-61461")
    fun nativeMetadataOfIntermediateWithMultipleTargets(gradleVersion: GradleVersion) {
        with(project("k2-native-intermediate-multiple-targets", gradleVersion)) {
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
        ) {
            val taskToExecute = ":compileCommonMainKotlinMetadata"
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @GradleTest
    @DisplayName("Common metadata compilation (expect actual discrimination). KT-61540")
    fun kt60438MetadataExpectActualDiscrimination(gradleVersion: GradleVersion) {
        project(
            "k2-kt-61540-expect-actual-discrimination", gradleVersion,
        ) {
            build("assemble") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
                assertTasksExecuted(":compileNativeMainKotlinMetadata")
                assertTasksExecuted(":compileLinuxMainKotlinMetadata")
            }
        }
    }

    @GradleTest
    @DisplayName("No overload resolution ambiguity between expect and non-expect in native")
    fun kt61778NoOverloadResolutionAmbiguityBetweenExpectAndNonExpectInNative(gradleVersion: GradleVersion) {
        project(
            "k2-no-overload-resolution-ambiguity-between-expect-and-non-expect-in-native", gradleVersion,
        ) {
            build("compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
            }
        }
    }

    @GradleTest
    @DisplayName("Native metadata compilation with constant expressions (KT-63835)")
    fun nativeMetadataCompilationWithConstantExpressions(gradleVersion: GradleVersion) {
        project("k2-native-metadata-compilation-with-constant-expressions", gradleVersion) {
            build("compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
            }
        }
    }
}

@NativeGradlePluginTests
@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("K2: custom MacOS tests")
class CustomK2MacOSTests : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions get() = super.defaultBuildOptions.copyEnsuringK2()

    @GradleTest
    @DisplayName("Universal metadata compilation with constant expressions (KT-63835)")
    fun universalMetadataCompilationWithConstantExpressions(gradleVersion: GradleVersion) {
        project("k2-universal-metadata-compilation-with-constant-expressions", gradleVersion) {
            build("assemble") {
                assertTasksExecuted(":assemble")
                assertTasksExecuted(":compileIosMainKotlinMetadata")
            }
        }
    }
}
