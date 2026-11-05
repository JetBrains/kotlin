/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import java.io.Serializable
import javax.inject.Inject

/**
 * Provisions a command line tool used by the Kotlin Gradle Plugin, for example D8, Binaryen, Wasmtime or SWC.
 *
 * The service accepts a [ToolchainRequest] - a description of the desired tool distribution - and returns
 * a [Provider] of a [ToolchainExecutable] that, when evaluated, points to a complete, provisioned installation.
 *
 * An implementation decides entirely how to satisfy the request: download the distribution,
 * reuse an already existing installation, or use the tool from the `PATH`.
 *
 * The service is realized as a shared [BuildService], so a single instance is used by the whole build,
 * and provisioning is safe with Gradle Isolated Projects, the configuration cache, and parallel execution.
 *
 * This is a part of the Kotlin Gradle Plugin public API - external users may provide their own implementation.
 * The Kotlin Gradle Plugin uses exactly one service per tool.
 *
 * @param P the type of the build service parameters. Subtypes may add their own parameters.
 */
@ExperimentalKotlinToolchainDsl
interface KotlinToolchainService<P : KotlinToolchainService.Parameters> : BuildService<P> {

    /**
     * Parameters of a [KotlinToolchainService].
     *
     * Implementations are expected to declare a subtype with the parameters they need.
     */
    interface Parameters : BuildServiceParameters

    /**
     * Requests a tool distribution described by [request].
     *
     * The returned provider is not evaluated eagerly - the distribution is only provisioned when the provider
     * value is queried, which for a task input happens after the configuration phase.
     */
    fun request(request: ToolchainRequest): Provider<ToolchainExecutable>
}

/**
 * Parameters of a toolchain service that downloads the tool distribution,
 * controlling where distributions are downloaded from and where they are installed.
 */
@ExperimentalKotlinToolchainDsl
abstract class DownloadedToolchainParameters : KotlinToolchainService.Parameters {

    /**
     * The directory containing all installations of the tool.
     *
     * Defaults to `<user.home>/.kotlin/toolchains/<tool>`.
     */
    abstract val installationDir: DirectoryProperty

    /**
     * The base URL the distributions are downloaded from.
     *
     * Defaults to the official distribution of the tool.
     */
    abstract val downloadBaseUrl: Property<String>

    /**
     * Whether the build is running in offline mode. It is set with the Gradle `--offline` option.
     *
     * When `true`, an already installed distribution is reused, and a missing one fails the build
     * instead of being downloaded.
     */
    abstract val offline: Property<Boolean>
}

/**
 * Parameters of a toolchain service that uses a tool already installed on the machine.
 */
@ExperimentalKotlinToolchainDsl
abstract class PreInstalledToolchainParameters : KotlinToolchainService.Parameters {

    /**
     * The command used to run the pre-installed tool, for example `wasm-opt` or a full path to the executable.
     */
    abstract val executable: Property<String>
}

/**
 * A description of the tool distribution a consumer needs.
 *
 * Instances are created by the Kotlin Gradle Plugin and read by a [KotlinToolchainService] implementation.
 */
@ExperimentalKotlinToolchainDsl
abstract class ToolchainRequest @Inject internal constructor() {

    /**
     * The requested tool version.
     *
     * There is intentionally no default value - the default version is defined per tool.
     */
    @get:Nested
    @get:Optional
    abstract val version: Property<ToolchainVersion>

    /**
     * The requested operating system and architecture.
     *
     * Defaults to the platform of the machine running the build.
     */
    @get:Nested
    @get:Optional
    abstract val platform: Property<ToolchainPlatform>

    /**
     * Sets [version] from a raw version string, for example `133`.
     */
    fun version(version: String) {
        this.version.set(ToolchainVersion(version))
    }
}

/**
 * A provisioned tool installation.
 *
 * Can be used as a task input:
 * the [executable] itself is not tracked, while the [version] and the [platform] are,
 * so that up-to-date checks do not depend on the machine-specific installation path.
 */
@ExperimentalKotlinToolchainDsl
abstract class ToolchainExecutable @Inject internal constructor() {

    /**
     * The tool executable.
     *
     * Either a full filesystem path to the executable, or just the command when the tool from the `PATH` is used.
     */
    @get:Internal
    abstract val executable: Property<String>

    /**
     * The version of the provisioned tool distribution.
     */
    @get:Nested
    abstract val version: Property<ToolchainVersion>

    /**
     * The platform of the provisioned tool distribution.
     */
    @get:Nested
    abstract val platform: Property<ToolchainPlatform>
}

/**
 * A tool version, for example `133`.
 *
 * @param version the version string. Must not be blank.
 */
@ExperimentalKotlinToolchainDsl
data class ToolchainVersion(
    @get:Input
    val version: String,
) : Serializable {
    init {
        require(version.isNotBlank()) { "Tool version must not be blank" }
    }

    private companion object {
        private const val serialVersionUID: Long = 0L
    }
}

/**
 * An operating system and architecture pair, using the Kotlin Native-like naming,
 * for example `linux` and `x86_64`.
 *
 * Every tool publishes its distributions under its own naming scheme, so the toolchain service
 * implementation of each tool maps this pair to the classifier the tool uses.
 *
 * @property os the operating system name, one of `linux`, `macos` or `windows`.
 * @property arch the CPU architecture, one of `x86_64`, `aarch64` or `x86`.
 */
@ExperimentalKotlinToolchainDsl
data class ToolchainPlatform(
    @get:Input
    val os: String,
    @get:Input
    val arch: String,
) : Serializable {

    /**
     * Returns the platform in the `<os>-<arch>` format, for example `linux-x86_64`.
     */
    override fun toString(): String = "$os-$arch"

    private companion object {
        private const val serialVersionUID: Long = 0L
    }
}
