/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.native.NativeExternalDependenciesIT.Companion.MASKED_TARGET_NAME
import org.jetbrains.kotlin.gradle.native.NativeExternalDependenciesIT.Companion.findKotlinNativeTargetName
import org.jetbrains.kotlin.gradle.native.NativeExternalDependenciesIT.Companion.findParameterInOutput
import org.jetbrains.kotlin.konan.library.KONAN_PLATFORM_LIBS_NAME_PREFIX
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NativeIrLinkerIssuesIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun `ktor 1_5_4 and coroutines 1_5_0-RC-native-mt (KT-46697)`() {
        // Run this test only on macOS x64,
        // arm64 requires newer ktor (>=1.6.5) and coroutines (>=1.5.2-native-mt)
        // that compatible to each other and don't fail this way.
        // TODO: consider finding a newer versions that support arm64 and reproduce this issue
        assumeTrue(HostManager.host == KonanTarget.MACOS_X64)

        buildApplicationAndFail(
            directoryPrefix = null,
            projectName = "native-ir-linker-issues-ktor-and-coroutines",
            localRepo = null,
            useCache = true
        ) { kotlinNativeCompilerVersion ->
            """
            |e: The symbol of unexpected type encountered during IR deserialization: IrTypeAliasPublicSymbolImpl, kotlinx.coroutines/CancellationException|null[0]. IrClassifierSymbol is expected.
            |
            |This could happen if there are two libraries, where one library was compiled against the different version of the other library than the one currently used in the project. Please check that the project configuration is correct and has consistent versions of dependencies.
            |
            |The list of libraries that depend on "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME)" and may lead to conflicts:
            |1. "io.ktor:ktor-client-core (io.ktor:ktor-client-core-$MASKED_TARGET_NAME): 1.5.4" (was compiled against "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.4.3-native-mt" but "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.5.0-RC-native-mt" is used in the project)
            |2. "io.ktor:ktor-http (io.ktor:ktor-http-$MASKED_TARGET_NAME): 1.5.4" (was compiled against "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.4.3-native-mt" but "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.5.0-RC-native-mt" is used in the project)
            |3. "io.ktor:ktor-http-cio (io.ktor:ktor-http-cio-$MASKED_TARGET_NAME): 1.5.4" (was compiled against "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.4.3-native-mt" but "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.5.0-RC-native-mt" is used in the project)
            |4. "io.ktor:ktor-io (io.ktor:ktor-io-$MASKED_TARGET_NAME): 1.5.4" (was compiled against "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.4.3-native-mt" but "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.5.0-RC-native-mt" is used in the project)
            |5. "io.ktor:ktor-utils (io.ktor:ktor-utils-$MASKED_TARGET_NAME): 1.5.4" (was compiled against "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.4.3-native-mt" but "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.5.0-RC-native-mt" is used in the project)
            |
            |Project dependencies:
            |+--- io.ktor:ktor-client-core (io.ktor:ktor-client-core-$MASKED_TARGET_NAME): 1.5.4
            ||    +--- io.ktor:ktor-http (io.ktor:ktor-http-$MASKED_TARGET_NAME): 1.5.4
            ||    |    +--- io.ktor:ktor-utils (io.ktor:ktor-utils-$MASKED_TARGET_NAME): 1.5.4
            ||    |    |    +--- io.ktor:ktor-io (io.ktor:ktor-io-$MASKED_TARGET_NAME): 1.5.4
            ||    |    |    |    +--- io.ktor:ktor-io-cinterop-bits: 1.5.4
            ||    |    |    |    |    \--- stdlib: 1.4.32 -> $kotlinNativeCompilerVersion
            ||    |    |    |    +--- io.ktor:ktor-io-cinterop-sockets: 1.5.4
            ||    |    |    |    |    \--- stdlib: 1.4.32 -> $kotlinNativeCompilerVersion
            ||    |    |    |    +--- stdlib: 1.4.32 -> $kotlinNativeCompilerVersion
            ||    |    |    |    +--- org.jetbrains.kotlin.native.platform.CoreFoundation: 1.4.32 -> $kotlinNativeCompilerVersion
            ||    |    |    |    |    +--- stdlib: $kotlinNativeCompilerVersion
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
            ||    |    |    |    +--- org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-$MASKED_TARGET_NAME): 0.15.1 -> 0.16.1
            ||    |    |    |    |    +--- stdlib: 1.5 -> $kotlinNativeCompilerVersion
            ||    |    |    |    |    +--- org.jetbrains.kotlin.native.platform.posix: 1.5 -> $kotlinNativeCompilerVersion (*)
            ||    |    |    |    |    \--- org.jetbrains.kotlinx:atomicfu-cinterop-interop: 0.16.1
            ||    |    |    |    |         +--- stdlib: 1.5 -> $kotlinNativeCompilerVersion
            ||    |    |    |    |         \--- org.jetbrains.kotlin.native.platform.posix: 1.5 -> $kotlinNativeCompilerVersion (*)
            ||    |    |    |    +--- org.jetbrains.kotlinx:atomicfu-cinterop-interop: 0.16.1 (*)
            ||    |    |    |    \--- org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.4.3-native-mt -> 1.5.0-RC-native-mt
            ||    |    |    |         ^^^ This module contains symbol kotlinx.coroutines/CancellationException|null[0] that is the cause of the conflict.
            ||    |    |    |         +--- stdlib: 1.5 -> $kotlinNativeCompilerVersion
            ||    |    |    |         +--- org.jetbrains.kotlin.native.platform.CoreFoundation: 1.5 -> $kotlinNativeCompilerVersion (*)
            ||    |    |    |         +--- org.jetbrains.kotlin.native.platform.darwin: 1.5 -> $kotlinNativeCompilerVersion (*)
            ||    |    |    |         +--- org.jetbrains.kotlin.native.platform.posix: 1.5 -> $kotlinNativeCompilerVersion (*)
            ||    |    |    |         +--- org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-$MASKED_TARGET_NAME): 0.16.1 (*)
            ||    |    |    |         \--- org.jetbrains.kotlinx:atomicfu-cinterop-interop: 0.16.1 (*)
            ||    |    |    +--- org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-$MASKED_TARGET_NAME): 0.15.1 -> 0.16.1 (*)
            ||    |    |    \--- org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.4.3-native-mt -> 1.5.0-RC-native-mt (*)
            ||    |    |         ^^^ This module contains symbol kotlinx.coroutines/CancellationException|null[0] that is the cause of the conflict.
            ||    |    +--- org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-$MASKED_TARGET_NAME): 0.15.1 -> 0.16.1 (*)
            ||    |    \--- org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.4.3-native-mt -> 1.5.0-RC-native-mt (*)
            ||    |         ^^^ This module contains symbol kotlinx.coroutines/CancellationException|null[0] that is the cause of the conflict.
            ||    +--- io.ktor:ktor-http-cio (io.ktor:ktor-http-cio-$MASKED_TARGET_NAME): 1.5.4
            ||    |    +--- io.ktor:ktor-http (io.ktor:ktor-http-$MASKED_TARGET_NAME): 1.5.4 (*)
            ||    |    +--- org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-$MASKED_TARGET_NAME): 0.15.1 -> 0.16.1 (*)
            ||    |    \--- org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.4.3-native-mt -> 1.5.0-RC-native-mt (*)
            ||    |         ^^^ This module contains symbol kotlinx.coroutines/CancellationException|null[0] that is the cause of the conflict.
            ||    +--- org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-$MASKED_TARGET_NAME): 0.15.1 -> 0.16.1 (*)
            ||    \--- org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.4.3-native-mt -> 1.5.0-RC-native-mt (*)
            ||         ^^^ This module contains symbol kotlinx.coroutines/CancellationException|null[0] that is the cause of the conflict.
            |\--- org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME): 1.5.0-RC-native-mt (*)
            |     ^^^ This module contains symbol kotlinx.coroutines/CancellationException|null[0] that is the cause of the conflict.
            |
            |(*) - dependencies omitted (listed previously)
            """.trimMargin()
        }
    }

    @Test
    fun `declaration that is gone (KT-41378) - with cache`() {
        // Don't run it on Windows. Caches are not supported there yet.
        assumeFalse(HostManager.hostIsMingw)

        buildConflictingLibrariesAndApplication(
            directoryPrefix = "native-ir-linker-issues-gone-declaration",
            useCache = true
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
            ||    \--- stdlib: $kotlinNativeCompilerVersion
            |\--- org.sample:libb (org.sample:libb-native): 1.0
            |     ^^^ This module requires symbol sample.liba/C|null[0].
            |     +--- org.sample:liba (org.sample:liba-native): 1.0 -> 2.0 (*)
            |     \--- stdlib: $kotlinNativeCompilerVersion
            |
            |(*) - dependencies omitted (listed previously)
            """.trimMargin()
        }
    }

    @Test
    fun `declaration that is gone (KT-41378) - without cache`() {
        buildConflictingLibrariesAndApplication(
            directoryPrefix = "native-ir-linker-issues-gone-declaration",
            useCache = false
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
            ||    \--- stdlib: $kotlinNativeCompilerVersion
            |+--- org.sample:libb (org.sample:libb-native): 1.0
            ||    ^^^ This module requires symbol sample.liba/C|null[0].
            ||    +--- org.sample:liba (org.sample:liba-native): 1.0 -> 2.0 (*)
            ||    \--- stdlib: $kotlinNativeCompilerVersion
            |\--- org.jetbrains.kotlin.native.platform.* (NNN libraries): $kotlinNativeCompilerVersion
            |     \--- stdlib: $kotlinNativeCompilerVersion
            |
            |(*) - dependencies omitted (listed previously)
            """.trimMargin()
        }
    }

    @Test
    fun `symbol type mismatch (KT-47285) - with cache`() {
        // Don't run it on Windows. Caches are not supported there yet.
        assumeFalse(HostManager.hostIsMingw)

        buildConflictingLibrariesAndApplication(
            directoryPrefix = "native-ir-linker-issues-symbol-mismatch",
            useCache = true
        ) { kotlinNativeCompilerVersion ->
            """
            |e: The symbol of unexpected type encountered during IR deserialization: IrTypeAliasPublicSymbolImpl, sample.liba/B|null[0]. IrClassifierSymbol is expected.
            |
            |This could happen if there are two libraries, where one library was compiled against the different version of the other library than the one currently used in the project. Please check that the project configuration is correct and has consistent versions of dependencies.
            |
            |The list of libraries that depend on "org.sample:liba (org.sample:liba-native)" and may lead to conflicts:
            |1. "org.sample:libb (org.sample:libb-native): 1.0" (was compiled against "org.sample:liba (org.sample:liba-native): 1.0" but "org.sample:liba (org.sample:liba-native): 2.0" is used in the project)
            |
            |Project dependencies:
            |+--- org.sample:liba (org.sample:liba-native): 2.0
            ||    ^^^ This module contains symbol sample.liba/B|null[0] that is the cause of the conflict.
            ||    \--- stdlib: $kotlinNativeCompilerVersion
            |\--- org.sample:libb (org.sample:libb-native): 1.0
            |     +--- org.sample:liba (org.sample:liba-native): 1.0 -> 2.0 (*)
            |     |    ^^^ This module contains symbol sample.liba/B|null[0] that is the cause of the conflict.
            |     \--- stdlib: $kotlinNativeCompilerVersion
            |
            |(*) - dependencies omitted (listed previously)
            """.trimMargin()
        }
    }

    @Test
    fun `symbol type mismatch (KT-47285) - without cache`() {
        buildConflictingLibrariesAndApplication(
            directoryPrefix = "native-ir-linker-issues-symbol-mismatch",
            useCache = false
        ) { kotlinNativeCompilerVersion ->
            """
            |e: The symbol of unexpected type encountered during IR deserialization: IrTypeAliasPublicSymbolImpl, sample.liba/B|null[0]. IrClassifierSymbol is expected.
            |
            |This could happen if there are two libraries, where one library was compiled against the different version of the other library than the one currently used in the project. Please check that the project configuration is correct and has consistent versions of dependencies.
            |
            |The list of libraries that depend on "org.sample:liba (org.sample:liba-native)" and may lead to conflicts:
            |1. "org.sample:libb (org.sample:libb-native): 1.0" (was compiled against "org.sample:liba (org.sample:liba-native): 1.0" but "org.sample:liba (org.sample:liba-native): 2.0" is used in the project)
            |
            |Project dependencies:
            |+--- org.sample:liba (org.sample:liba-native): 2.0
            ||    ^^^ This module contains symbol sample.liba/B|null[0] that is the cause of the conflict.
            ||    \--- stdlib: $kotlinNativeCompilerVersion
            |+--- org.sample:libb (org.sample:libb-native): 1.0
            ||    +--- org.sample:liba (org.sample:liba-native): 1.0 -> 2.0 (*)
            ||    |    ^^^ This module contains symbol sample.liba/B|null[0] that is the cause of the conflict.
            ||    \--- stdlib: $kotlinNativeCompilerVersion
            |\--- org.jetbrains.kotlin.native.platform.* (NNN libraries): $kotlinNativeCompilerVersion
            |     \--- stdlib: $kotlinNativeCompilerVersion
            |
            |(*) - dependencies omitted (listed previously)
            """.trimMargin()
        }
    }

    private fun buildConflictingLibrariesAndApplication(
        directoryPrefix: String,
        useCache: Boolean,
        expectedErrorMessage: (compilerVersion: String) -> String
    ) {
        val repo = setupLocalRepo()

        buildAndPublishLibrary(directoryPrefix = directoryPrefix, projectName = "liba-v1.0", localRepo = repo)
        buildAndPublishLibrary(directoryPrefix = directoryPrefix, projectName = "liba-v2.0", localRepo = repo)
        buildAndPublishLibrary(directoryPrefix = directoryPrefix, projectName = "libb", localRepo = repo)

        buildApplicationAndFail(
            directoryPrefix = directoryPrefix,
            projectName = "app",
            localRepo = repo,
            useCache = useCache,
            expectedErrorMessage = expectedErrorMessage
        )
    }

    private fun buildApplicationAndFail(
        directoryPrefix: String?,
        projectName: String,
        localRepo: File?,
        useCache: Boolean,
        expectedErrorMessage: (compilerVersion: String) -> String
    ) {
        prepareProject(directoryPrefix, projectName, localRepo, useCache) {
            build("linkDebugExecutableNative") {
                assertFailed()

                val kotlinNativeTargetName = findKotlinNativeTargetName(output)
                assertNotNull(kotlinNativeTargetName)

                val kotlinNativeCompilerVersion = findKotlinNativeCompilerVersion(output)
                assertNotNull(kotlinNativeCompilerVersion)

                val errorMessage = ERROR_LINE_REGEX.findAll(getOutputForTask("linkDebugExecutableNative"))
                    .map { matchResult -> matchResult.groupValues[1] }
                    .map { line -> line.replace(kotlinNativeTargetName, MASKED_TARGET_NAME) }
                    .map { line ->
                        line.replace(COMPRESSED_PLATFORM_LIBS_REGEX) { result ->
                            val rangeWithPlatformLibrariesCount = result.groups[1]!!.range
                            buildString {
                                append(line.substring(0, rangeWithPlatformLibrariesCount.first))
                                append("NNN")
                                append(line.substring(rangeWithPlatformLibrariesCount.last + 1))
                            }
                        }
                    }
                    .joinToString("\n")

                assertEquals(expectedErrorMessage(kotlinNativeCompilerVersion), errorMessage)
            }
        }
    }

    private fun buildAndPublishLibrary(directoryPrefix: String, projectName: String, localRepo: File) {
        prepareProject(directoryPrefix, projectName, localRepo, useCache = true) {
            build("publish") {
                assertSuccessful()
            }
        }
    }

    private fun prepareProject(
        directoryPrefix: String?,
        projectName: String,
        localRepo: File?,
        useCache: Boolean,
        block: Project.() -> Unit
    ) {
        with(transformNativeTestProjectWithPluginDsl(directoryPrefix = directoryPrefix, projectName = projectName)) {
            if (localRepo != null) {
                val localRepoUri = localRepo.absoluteFile.toURI().toString()
                gradleBuildScript().apply {
                    writeText(readText().replace("<LocalRepo>", localRepoUri))
                }
            }

            gradleProperties().appendText("\nkotlin.native.cacheKind=${if (useCache) "static" else "none"}\n")

            block()
        }
    }

    companion object {
        private val ERROR_LINE_REGEX = "(?m)^.*\\[ERROR] \\[\\S+] (.*)$".toRegex()
        private val COMPRESSED_PLATFORM_LIBS_REGEX =
            ".*${KONAN_PLATFORM_LIBS_NAME_PREFIX.replace(".", "\\.")}\\* \\((\\d+) libraries\\).*".toRegex()

        private fun setupLocalRepo(): File = Files.createTempDirectory("localRepo").toAbsolutePath().toFile()

        fun findKotlinNativeCompilerVersion(output: String): String? = findParameterInOutput(
            "for_test_kotlin_native_compiler_version",
            output
        )
    }
}
