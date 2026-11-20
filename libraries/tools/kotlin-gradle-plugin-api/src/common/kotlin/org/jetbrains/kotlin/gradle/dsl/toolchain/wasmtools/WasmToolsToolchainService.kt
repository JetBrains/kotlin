/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.io.Serializable
import javax.inject.Inject

/**
 * Provisions wasm-tools distributions for the Kotlin Gradle Plugin.
 *
 * The service accepts a [WasmToolsRequest] - a description of the desired wasm-tools distribution - and returns
 * a [Provider] of a [WasmToolsExecutable] that, when evaluated, points to a complete, provisioned installation.
 *
 * An implementation decides entirely how to satisfy the request: download the distribution,
 * reuse an already existing installation, or use `wasm-tools` from the `PATH`.
 *
 * The service is realized as a shared [BuildService], so a single instance is used by the whole build,
 * and provisioning is safe with Gradle Isolated Projects, the configuration cache, and parallel execution.
 *
 * This is a part of the Kotlin Gradle Plugin public API - external users may provide their own implementation.
 * The Kotlin Gradle Plugin uses exactly one service, and ships the following built-in implementations:
 * - [DefaultWasmToolsToolchainService] - downloads the official wasm-tools distribution.
 * - [PreInstalledWasmToolsToolchainService] - uses `wasm-tools` available on the `PATH`.
 *
 * A custom implementation may delegate to one of the built-in ones.
 *
 * @param P the type of the build service parameters. Subtypes may add their own parameters.
 */
@ExperimentalWasmDsl
interface WasmToolsToolchainService<P : WasmToolsToolchainService.Parameters> : BuildService<P> {

    /**
     * Parameters of a [WasmToolsToolchainService].
     *
     * Implementations are expected to declare a subtype with the parameters they need.
     */
    interface Parameters : BuildServiceParameters

    /**
     * Requests a wasm-tools distribution described by [wasmToolsRequest].
     *
     * The returned provider is not evaluated eagerly - the distribution is only provisioned when the provider
     * value is queried, which for a task input happens after the configuration phase.
     */
    fun request(wasmToolsRequest: WasmToolsRequest): Provider<WasmToolsExecutable>
}

/**
 * A description of the wasm-tools distribution a consumer needs.
 *
 * Instances are created by the Kotlin Gradle Plugin and read by a [WasmToolsToolchainService] implementation.
 */
@ExperimentalWasmDsl
abstract class WasmToolsRequest @Inject internal constructor() {
    /**
     * The requested wasm-tools version.
     *
     * There is intentionally no default value - the default version is defined per use case
     * (for example, resolving npm dependencies, or running tests).
     */
    @get:Input
    abstract val version: Property<WasmToolsVersion>

    /**
     * The requested operating system and architecture.
     *
     * Defaults to the platform of the machine running the build.
     */
    @get:Input
    abstract val platform: Property<WasmToolsBuildPlatform>

    /**
     * Sets [version] from a raw wasm-tools version string, for example `1.0.0`.
     */
    fun version(version: String) {
        this.version.set(WasmToolsVersion(version))
    }
}

/**
 * A provisioned wasm-tools installation.
 *
 * Can be used as a task input:
 * the [executable] itself is not tracked, while the [version] and the [platform] are,
 * so that up-to-date checks do not depend on the machine-specific installation path.
 */
@ExperimentalWasmDsl
abstract class WasmToolsExecutable @Inject internal constructor() {
    /**
     * The wasm-tools executable.
     *
     * Either a full filesystem path to the executable, or just `wasm-tools` when `wasm-tools` from the `PATH` is used.
     */
    @get:Internal
    abstract val executable: Property<String>

    /**
     * The version of the provisioned wasm-tools distribution.
     */
    @get:Nested
    abstract val version: Property<WasmToolsVersion>

    /**
     * The platform of the provisioned wasm-tools distribution.
     */
    @get:Nested
    abstract val platform: Property<WasmToolsBuildPlatform>
}

/**
 * A wasm-tools version, for example `1.0.0`.
 *
 * @property version the version string. Must not be blank.
 */
@ExperimentalWasmDsl
sealed interface WasmToolsVersion : Serializable {
    /**
     * Represents the version of the wasm-tools.
     *
     * The version is specified as a non-blank string, typically in the format of `MAJOR.MINOR.PATCH`
     * (e.g., `1.0.0`).
     */
    @get:Input
    val version: String
}

@ExperimentalWasmDsl
private data class WasmToolsVersionImpl(
    override val version: String
) : WasmToolsVersion {
    init {
        require(version.isNotBlank()) { "wasm-tools version must not be blank" }
    }

    override fun toString(): String = version
}

/**
 * Creates a [WasmToolsVersion] from the given [version] string, for example `1.0.0`.
 *
 * @param version the wasm-tools version. Must not be blank.
 * @throws IllegalArgumentException if [version] is blank.
 */
@ExperimentalWasmDsl
fun WasmToolsVersion(version: String): WasmToolsVersion =
    WasmToolsVersionImpl(version)

/**
 * An operating system and architecture pair, using the wasm-tools distribution naming,
 * for example `linux` and `x64`.
 *
 * @property os the operating system name, for example `linux`, `darwin` or `win`.
 * @property arch the CPU architecture, for example `x64` or `arm64`.
 */
@ExperimentalWasmDsl
sealed interface WasmToolsBuildPlatform : Serializable {

    /**
     * Represents the operating system name for a given platform in the wasm-tools distribution naming convention.
     *
     * The `os` variable specifies the operating system being targeted or used and is expected
     * to contain values like `linux`, `darwin`, or `win` depending on the supported platforms.
     */
    @get:Input
    val os: String

    /**
     * Represents the CPU architecture for a given platform in the wasm-tools distribution naming convention.
     *
     * The `arch` variable specifies the target CPU architecture and is expected to contain values like `x64` or `arm64`
     * for supported architectures.
     */
    @get:Input
    val arch: String
}

@ExperimentalWasmDsl
private data class WasmToolsBuildPlatformImpl(
    override val os: String,
    override val arch: String,
) : WasmToolsBuildPlatform {
    /**
     * Returns the platform in the wasm-tools distribution naming format `<os>-<arch>`, for example `linux-x64`.
     */
    override fun toString(): String = "$os-$arch"
}

/**
 * Creates a [WasmToolsBuildPlatform] for the given [os] and [arch],
 * using the wasm-tools distribution naming, for example `linux` and `x64`.
 *
 * @param os the operating system name, for example `linux`, `darwin` or `win`.
 * @param arch the CPU architecture, for example `x64` or `arm64`.
 */
@ExperimentalWasmDsl
fun WasmToolsBuildPlatform(os: String, arch: String): WasmToolsBuildPlatform =
    WasmToolsBuildPlatformImpl(os, arch)
