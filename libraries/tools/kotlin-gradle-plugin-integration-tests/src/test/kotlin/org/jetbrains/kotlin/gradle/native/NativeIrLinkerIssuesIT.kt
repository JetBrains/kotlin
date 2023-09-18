/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.library.KONAN_PLATFORM_LIBS_NAME_PREFIX
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DisplayName("Tests for K/N builds with ir linker issues")
@NativeGradlePluginTests
internal class NativeIrLinkerIssuesIT : KGPBaseTest() {

    @DisplayName("KT-46697: ktor 1_5_4 and coroutines 1_5_0-RC-native-mt")
    @GradleTest
    @Disabled(
        "This sample fails in a way that was not expected because kotlin/Experimental annotation that is still used in ktor 1.5.4" +
                " was removed in stdlib in 1.8. We need to find another appropriate sample to replace this one."
    )
    // TODO: consider finding a newer versions that support arm64 and reproduce this issue
    @DisabledOnOs(
        OS.MAC,
        architectures = ["aarch64"],
        disabledReason =
        "Run this test only on macOS x64," +
                " arm64 requires newer ktor (>=1.6.5) and coroutines (>=1.5.2-native-mt) " +
                "that compatible to each other and don't fail this way."
    )
    fun shouldBuildKtorAndCoroutines(gradleVersion: GradleVersion) {

        buildApplicationAndFail(
            directoryPrefix = null,
            projectName = "native-ir-linker-issues-ktor-and-coroutines",
            localRepo = null,
            nativeCacheKind = NativeCacheKind.STATIC,
            gradleVersion = gradleVersion
        ) { kotlinNativeCompilerVersion ->
            """
            |e: The symbol of unexpected type encountered during IR deserialization: IrTypeAliasPublicSymbolImpl, kotlinx.coroutines/CancellationException|null[0]. IrClassifierSymbol is expected.
            |
            |This could happen if there are two libraries, where one library was compiled against the different version of the other library than the one currently used in the project. Please check that the project configuration is correct and has consistent versions of dependencies.
            |
            |The list of libraries that depend on "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX)" and may lead to conflicts:
            |1. "io.ktor:ktor-client-core (io.ktor:ktor-client-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.4" (was compiled against "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.4.3-native-mt" but "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.0-RC-native-mt" is used in the project)
            |2. "io.ktor:ktor-http (io.ktor:ktor-http-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.4" (was compiled against "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.4.3-native-mt" but "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.0-RC-native-mt" is used in the project)
            |3. "io.ktor:ktor-http-cio (io.ktor:ktor-http-cio-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.4" (was compiled against "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.4.3-native-mt" but "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.0-RC-native-mt" is used in the project)
            |4. "io.ktor:ktor-io (io.ktor:ktor-io-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.4" (was compiled against "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.4.3-native-mt" but "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.0-RC-native-mt" is used in the project)
            |5. "io.ktor:ktor-utils (io.ktor:ktor-utils-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.4" (was compiled against "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.4.3-native-mt" but "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.0-RC-native-mt" is used in the project)
            |
            |Project dependencies:
            |+--- io.ktor:ktor-client-core (io.ktor:ktor-client-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.4
            ||    +--- io.ktor:ktor-http (io.ktor:ktor-http-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.4
            ||    |    +--- io.ktor:ktor-utils (io.ktor:ktor-utils-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.4
            ||    |    |    +--- io.ktor:ktor-io (io.ktor:ktor-io-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.4
            ||    |    |    |    +--- io.ktor:ktor-io-cinterop-bits: 1.5.4
            ||    |    |    |    |    \--- stdlib: 1.4.32 -> $kotlinNativeCompilerVersion
            ||    |    |    |    +--- io.ktor:ktor-io-cinterop-sockets: 1.5.4
            ||    |    |    |    |    \--- stdlib: 1.4.32 -> $kotlinNativeCompilerVersion
            ||    |    |    |    +--- stdlib: 1.4.32 -> $kotlinNativeCompilerVersion
            ||    |    |    |    +--- org.jetbrains.kotlin.native.platform.CoreFoundation: 1.4.32 -> $kotlinNativeCompilerVersion
            ||    |    |    |    |    +--- stdlib: $kotlinNativeCompilerVersion
            ||    |    |    |    |    +--- org.jetbrains.kotlin.native.platform.CoreFoundationBase: $kotlinNativeCompilerVersion
            ||    |    |    |    |    |    \--- stdlib: $kotlinNativeCompilerVersion
            ||    |    |    |    |    +--- org.jetbrains.kotlin.native.platform.darwin: $kotlinNativeCompilerVersion
            ||    |    |    |    |    |    +--- stdlib: $kotlinNativeCompilerVersion
            ||    |    |    |    |    |    \--- org.jetbrains.kotlin.native.platform.posix: $kotlinNativeCompilerVersion
            ||    |    |    |    |    |         \--- stdlib: $kotlinNativeCompilerVersion
            ||    |    |    |    |    \--- org.jetbrains.kotlin.native.platform.posix: $kotlinNativeCompilerVersion (*)
            ||    |    |    |    +--- org.jetbrains.kotlin.native.platform.darwin: 1.4.32 -> $kotlinNativeCompilerVersion (*)
            ||    |    |    |    +--- org.jetbrains.kotlin.native.platform.iconv: 1.4.32 -> $kotlinNativeCompilerVersion
            ||    |    |    |    |    +--- stdlib: $kotlinNativeCompilerVersion
            ||    |    |    |    |    \--- org.jetbrains.kotlin.native.platform.posix: $kotlinNativeCompilerVersion (*)
            ||    |    |    |    +--- org.jetbrains.kotlin.native.platform.posix: 1.4.32 -> $kotlinNativeCompilerVersion (*)
            ||    |    |    |    +--- org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 0.15.1 -> 0.16.1
            ||    |    |    |    |    +--- stdlib: 1.5 -> $kotlinNativeCompilerVersion
            ||    |    |    |    |    +--- org.jetbrains.kotlin.native.platform.posix: 1.5 -> $kotlinNativeCompilerVersion (*)
            ||    |    |    |    |    \--- org.jetbrains.kotlinx:atomicfu-cinterop-interop: 0.16.1
            ||    |    |    |    |         +--- stdlib: 1.5 -> $kotlinNativeCompilerVersion
            ||    |    |    |    |         \--- org.jetbrains.kotlin.native.platform.posix: 1.5 -> $kotlinNativeCompilerVersion (*)
            ||    |    |    |    +--- org.jetbrains.kotlinx:atomicfu-cinterop-interop: 0.16.1 (*)
            ||    |    |    |    \--- org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.4.3-native-mt -> 1.5.0-RC-native-mt
            ||    |    |    |         ^^^ This module contains symbol kotlinx.coroutines/CancellationException|null[0] that is the cause of the conflict.
            ||    |    |    |         +--- stdlib: 1.5 -> $kotlinNativeCompilerVersion
            ||    |    |    |         +--- org.jetbrains.kotlin.native.platform.CoreFoundation: 1.5 -> $kotlinNativeCompilerVersion (*)
            ||    |    |    |         +--- org.jetbrains.kotlin.native.platform.darwin: 1.5 -> $kotlinNativeCompilerVersion (*)
            ||    |    |    |         +--- org.jetbrains.kotlin.native.platform.posix: 1.5 -> $kotlinNativeCompilerVersion (*)
            ||    |    |    |         +--- org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 0.16.1 (*)
            ||    |    |    |         \--- org.jetbrains.kotlinx:atomicfu-cinterop-interop: 0.16.1 (*)
            ||    |    |    +--- org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 0.15.1 -> 0.16.1 (*)
            ||    |    |    \--- org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.4.3-native-mt -> 1.5.0-RC-native-mt (*)
            ||    |    |         ^^^ This module contains symbol kotlinx.coroutines/CancellationException|null[0] that is the cause of the conflict.
            ||    |    +--- org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 0.15.1 -> 0.16.1 (*)
            ||    |    \--- org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.4.3-native-mt -> 1.5.0-RC-native-mt (*)
            ||    |         ^^^ This module contains symbol kotlinx.coroutines/CancellationException|null[0] that is the cause of the conflict.
            ||    +--- io.ktor:ktor-http-cio (io.ktor:ktor-http-cio-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.4
            ||    |    +--- io.ktor:ktor-http (io.ktor:ktor-http-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.4 (*)
            ||    |    +--- org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 0.15.1 -> 0.16.1 (*)
            ||    |    \--- org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.4.3-native-mt -> 1.5.0-RC-native-mt (*)
            ||    |         ^^^ This module contains symbol kotlinx.coroutines/CancellationException|null[0] that is the cause of the conflict.
            ||    +--- org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 0.15.1 -> 0.16.1 (*)
            ||    \--- org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.4.3-native-mt -> 1.5.0-RC-native-mt (*)
            ||         ^^^ This module contains symbol kotlinx.coroutines/CancellationException|null[0] that is the cause of the conflict.
            |\--- org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX): 1.5.0-RC-native-mt (*)
            |     ^^^ This module contains symbol kotlinx.coroutines/CancellationException|null[0] that is the cause of the conflict.
            |
            |(*) - dependencies omitted (listed previously)
            """.trimMargin()
        }
    }

    @DisplayName("KT-41378: declaration that is gone - with cache")
    @GradleTest
    @DisabledOnOs(OS.WINDOWS, disabledReason = "Don't run it on Windows. Caches are not supported there yet.")
    fun shouldBuildIrLinkerWithCache(
        gradleVersion: GradleVersion,
        @TempDir tempDir: Path,
    ) {

        buildConflictingLibrariesAndApplication(
            directoryPrefix = "native-ir-linker-issues-gone-declaration",
            nativeCacheKind = NativeCacheKind.STATIC,
            gradleVersion = gradleVersion,
            localRepo = tempDir
        ) { kotlinNativeCompilerVersion ->
            """
            |e: Module "org.sample:libb (org.sample:libb-native)" has a reference to symbol sample.liba/C|null[0]. Neither the module itself nor its dependencies contain such declaration.
            |
            |This could happen if the required dependency is missing in the project. Or if there is a dependency of "org.sample:libb (org.sample:libb-native)" that has a different version in the project than the version that "org.sample:libb (org.sample:libb-native): 1.0" was initially compiled with. Please check that the project configuration is correct and has consistent versions of all required dependencies.
            |
            |The list of "org.sample:libb (org.sample:libb-native): 1.0" dependencies that may lead to conflicts:
            |1. "org.sample:liba (org.sample:liba-native): 2.0" (was initially compiled with "org.sample:liba (org.sample:liba-native): 1.0")
            |
            |Project dependencies:
            |+--- org.sample:liba (org.sample:liba-native): 2.0
            ||    +--- stdlib: $kotlinNativeCompilerVersion
            ||    \--- org.jetbrains.kotlin:kotlin-stdlib: $KOTLIN_VERSION
            |+--- org.sample:libb (org.sample:libb-native): 1.0
            ||    ^^^ This module requires symbol sample.liba/C|null[0].
            ||    +--- org.sample:liba (org.sample:liba-native): 1.0 -> 2.0 (*)
            ||    +--- stdlib: $kotlinNativeCompilerVersion
            ||    \--- org.jetbrains.kotlin:kotlin-stdlib: $KOTLIN_VERSION
            |\--- org.jetbrains.kotlin:kotlin-stdlib: $KOTLIN_VERSION
            |
            |(*) - dependencies omitted (listed previously)
            """.trimMargin()
        }
    }

    @DisplayName("KT-41378: declaration that is gone - without cache")
    @GradleTest
    fun shouldBuildIrLinkerWithoutCache(
        gradleVersion: GradleVersion,
        @TempDir tempDir: Path,
    ) {
        buildConflictingLibrariesAndApplication(
            directoryPrefix = "native-ir-linker-issues-gone-declaration",
            nativeCacheKind = NativeCacheKind.NONE,
            gradleVersion = gradleVersion,
            localRepo = tempDir
        ) { kotlinNativeCompilerVersion ->
            """
            e: Module "org.sample:libb (org.sample:libb-native)" has a reference to symbol sample.liba/C|null[0]. Neither the module itself nor its dependencies contain such declaration.
            
            This could happen if the required dependency is missing in the project. Or if there is a dependency of "org.sample:libb (org.sample:libb-native)" that has a different version in the project than the version that "org.sample:libb (org.sample:libb-native): 1.0" was initially compiled with. Please check that the project configuration is correct and has consistent versions of all required dependencies.
            
            The list of "org.sample:libb (org.sample:libb-native): 1.0" dependencies that may lead to conflicts:
            1. "org.sample:liba (org.sample:liba-native): 2.0" (was initially compiled with "org.sample:liba (org.sample:liba-native): 1.0")
            
            Project dependencies:
            +--- org.sample:liba (org.sample:liba-native): 2.0
            |    +--- stdlib: $kotlinNativeCompilerVersion
            |    \--- org.jetbrains.kotlin:kotlin-stdlib: $KOTLIN_VERSION
            +--- org.sample:libb (org.sample:libb-native): 1.0
            |    ^^^ This module requires symbol sample.liba/C|null[0].
            |    +--- org.sample:liba (org.sample:liba-native): 1.0 -> 2.0 (*)
            |    +--- stdlib: $kotlinNativeCompilerVersion
            |    \--- org.jetbrains.kotlin:kotlin-stdlib: $KOTLIN_VERSION
            +--- org.jetbrains.kotlin.native.platform.* (NNN libraries): $kotlinNativeCompilerVersion
            |    \--- stdlib: $kotlinNativeCompilerVersion
            \--- org.jetbrains.kotlin:kotlin-stdlib: $KOTLIN_VERSION
            
            (*) - dependencies omitted (listed previously)
            """.trimIndent()
        }
    }

    @DisplayName("KT-47285: symbol type mismatch - with cache")
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC, OS.LINUX], enabledOnCI = [OS.LINUX])
    // Don't run it on Windows. Caches are not supported there yet.
    fun shouldBuildIrLinkerSymbolTypeMismatchWithCache(
        gradleVersion: GradleVersion,
        @TempDir tempDir: Path,
    ) {
        buildConflictingLibrariesAndApplication(
            directoryPrefix = "native-ir-linker-issues-symbol-mismatch",
            nativeCacheKind = NativeCacheKind.STATIC,
            gradleVersion = gradleVersion,
            localRepo = tempDir
        ) { kotlinNativeCompilerVersion ->
            """
            e: The symbol of unexpected type encountered during IR deserialization: IrClassPublicSymbolImpl, sample.liba/B|null[0]. IrTypeAliasSymbol is expected.
            
            This could happen if there are two libraries, where one library was compiled against the different version of the other library than the one currently used in the project. Please check that the project configuration is correct and has consistent versions of dependencies.
            
            The list of libraries that depend on "org.sample:liba (org.sample:liba-native)" and may lead to conflicts:
            1. "org.sample:libb (org.sample:libb-native): 1.0" (was compiled against "org.sample:liba (org.sample:liba-native): 1.0" but "org.sample:liba (org.sample:liba-native): 2.0" is used in the project)
            
            Project dependencies:
            +--- org.sample:liba (org.sample:liba-native): 2.0
            |    ^^^ This module contains symbol sample.liba/B|null[0] that is the cause of the conflict.
            |    +--- stdlib: $kotlinNativeCompilerVersion
            |    \--- org.jetbrains.kotlin:kotlin-stdlib: $KOTLIN_VERSION
            +--- org.sample:libb (org.sample:libb-native): 1.0
            |    +--- org.sample:liba (org.sample:liba-native): 1.0 -> 2.0 (*)
            |    |    ^^^ This module contains symbol sample.liba/B|null[0] that is the cause of the conflict.
            |    +--- stdlib: $kotlinNativeCompilerVersion
            |    \--- org.jetbrains.kotlin:kotlin-stdlib: $KOTLIN_VERSION
            \--- org.jetbrains.kotlin:kotlin-stdlib: $KOTLIN_VERSION

            (*) - dependencies omitted (listed previously)
            """.trimIndent()
        }
    }

    @DisplayName("KT-47285: symbol type mismatch - without cache")
    @GradleTest
    fun shouldBuildIrLinkerSymbolTypeMismatchWithoutCache(
        gradleVersion: GradleVersion,
        @TempDir tempDir: Path,
    ) {
        buildConflictingLibrariesAndApplication(
            directoryPrefix = "native-ir-linker-issues-symbol-mismatch",
            nativeCacheKind = NativeCacheKind.NONE,
            gradleVersion = gradleVersion,
            localRepo = tempDir
        ) { kotlinNativeCompilerVersion ->
            """
            e: The symbol of unexpected type encountered during IR deserialization: IrClassPublicSymbolImpl, sample.liba/B|null[0]. IrTypeAliasSymbol is expected.
            
            This could happen if there are two libraries, where one library was compiled against the different version of the other library than the one currently used in the project. Please check that the project configuration is correct and has consistent versions of dependencies.
            
            The list of libraries that depend on "org.sample:liba (org.sample:liba-native)" and may lead to conflicts:
            1. "org.sample:libb (org.sample:libb-native): 1.0" (was compiled against "org.sample:liba (org.sample:liba-native): 1.0" but "org.sample:liba (org.sample:liba-native): 2.0" is used in the project)
            
            Project dependencies:
            +--- org.sample:liba (org.sample:liba-native): 2.0
            |    ^^^ This module contains symbol sample.liba/B|null[0] that is the cause of the conflict.
            |    +--- stdlib: $kotlinNativeCompilerVersion
            |    \--- org.jetbrains.kotlin:kotlin-stdlib: $KOTLIN_VERSION
            +--- org.sample:libb (org.sample:libb-native): 1.0
            |    +--- org.sample:liba (org.sample:liba-native): 1.0 -> 2.0 (*)
            |    |    ^^^ This module contains symbol sample.liba/B|null[0] that is the cause of the conflict.
            |    +--- stdlib: $kotlinNativeCompilerVersion
            |    \--- org.jetbrains.kotlin:kotlin-stdlib: $KOTLIN_VERSION
            +--- org.jetbrains.kotlin.native.platform.* (NNN libraries): $kotlinNativeCompilerVersion
            |    \--- stdlib: $kotlinNativeCompilerVersion
            \--- org.jetbrains.kotlin:kotlin-stdlib: ${KOTLIN_VERSION}
            
            (*) - dependencies omitted (listed previously)
            """.trimIndent()
        }
    }

    private fun buildConflictingLibrariesAndApplication(
        directoryPrefix: String,
        nativeCacheKind: NativeCacheKind,
        gradleVersion: GradleVersion,
        localRepo: Path,
        expectedErrorMessage: (compilerVersion: String) -> String,
    ) {
        buildAndPublishLibrary(directoryPrefix = directoryPrefix, projectName = "liba-v1.0", localRepo = localRepo, gradleVersion)
        buildAndPublishLibrary(directoryPrefix = directoryPrefix, projectName = "liba-v2.0", localRepo = localRepo, gradleVersion)
        buildAndPublishLibrary(directoryPrefix = directoryPrefix, projectName = "libb", localRepo = localRepo, gradleVersion)

        buildApplicationAndFail(
            directoryPrefix = directoryPrefix,
            projectName = "app",
            localRepo = localRepo,
            nativeCacheKind = nativeCacheKind,
            gradleVersion = gradleVersion,
            expectedErrorMessage = expectedErrorMessage
        )
    }

    private fun buildApplicationAndFail(
        directoryPrefix: String?,
        projectName: String,
        localRepo: Path?,
        nativeCacheKind: NativeCacheKind,
        gradleVersion: GradleVersion,
        expectedErrorMessage: (compilerVersion: String) -> String,
    ) {
        prepareProject(directoryPrefix, projectName, localRepo, nativeCacheKind, gradleVersion) {
            buildAndFail("linkDebugExecutableNative", buildOptions = this.buildOptions.copy(logLevel = LogLevel.DEBUG)) {

                val kotlinNativeCompilerVersion = findKotlinNativeCompilerVersion(output)
                assertNotNull(kotlinNativeCompilerVersion)

                val errorMessage = ERROR_LINE_REGEX.findAll(getOutputForTask(":linkDebugExecutableNative"))
                    .map { matchResult -> matchResult.groupValues[1] }
                    .filterNot { it.startsWith("w:") || it.startsWith("v:") || it.startsWith("i:") }.map { line ->
                        line.replace(COMPRESSED_PLATFORM_LIBS_REGEX) { result ->
                            val rangeWithPlatformLibrariesCount = result.groups[1]!!.range
                            buildString {
                                append(line.substring(0, rangeWithPlatformLibrariesCount.first))
                                append("NNN")
                                append(line.substring(rangeWithPlatformLibrariesCount.last + 1))
                            }
                        }
                    }.joinToString("\n")

                assertEquals(expectedErrorMessage(kotlinNativeCompilerVersion), errorMessage)
            }
        }
    }

    private fun buildAndPublishLibrary(
        directoryPrefix: String, projectName: String, localRepo: Path, gradleVersion: GradleVersion,
    ) {
        prepareProject(directoryPrefix, projectName, localRepo, nativeCacheKind = NativeCacheKind.STATIC, gradleVersion) {
            build("publish")
        }
    }

    private fun prepareProject(
        directoryPrefix: String?,
        projectName: String,
        localRepo: Path?,
        nativeCacheKind: NativeCacheKind,
        gradleVersion: GradleVersion,
        block: TestProject.() -> Unit,
    ) {
        nativeProject(
            directoryPrefix + "/" + projectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                nativeOptions = defaultBuildOptions.nativeOptions.copy(cacheKind = nativeCacheKind)),
            localRepoDir = localRepo
        ) {
            block()
        }
    }

    companion object {
        private val ERROR_LINE_REGEX = "(?m)^.*\\[ERROR] \\[\\S+] (.*)$".toRegex()
        private val COMPRESSED_PLATFORM_LIBS_REGEX =
            ".*${KONAN_PLATFORM_LIBS_NAME_PREFIX.replace(".", "\\.")}\\* \\((\\d+) libraries\\).*".toRegex()

        private fun findKotlinNativeCompilerVersion(output: String): String? = findParameterInOutput(
            "for_test_kotlin_native_compiler_version",
            output
        )
    }
}
