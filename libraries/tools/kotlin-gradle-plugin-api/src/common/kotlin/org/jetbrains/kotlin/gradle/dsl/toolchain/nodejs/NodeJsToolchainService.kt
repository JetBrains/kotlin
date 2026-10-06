/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import java.io.Serializable
import javax.inject.Inject

/**
 * Provisions Node.js distributions for the Kotlin Gradle Plugin.
 *
 * The service accepts a [NodeJsRequest] - a description of the desired Node.js distribution - and returns
 * a [Provider] of a [NodeJsExecutable] that, when evaluated, points to a complete, provisioned installation.
 *
 * An implementation decides entirely how to satisfy the request: download the distribution,
 * reuse an already existing installation, or use `node` from the `PATH`.
 *
 * The service is realized as a shared [BuildService], so a single instance is used by the whole build,
 * and provisioning is safe with Gradle Isolated Projects, the configuration cache, and parallel execution.
 *
 * This is a part of the Kotlin Gradle Plugin public API - external users may provide their own implementation.
 * The Kotlin Gradle Plugin uses exactly one service, and ships the following built-in implementations:
 * - [DefaultNodeJsToolchainService] - downloads the official Node.js distribution.
 * - [PreInstalledNodeJsToolchainService] - uses `node` available on the `PATH`.
 *
 * A custom implementation may delegate to one of the built-in ones.
 *
 * @param P the type of the build service parameters. Subtypes may add their own parameters.
 */
@ExperimentalNodeJsToolchainDsl
interface NodeJsToolchainService<P : NodeJsToolchainService.Parameters> : BuildService<P> {

    /**
     * Parameters of a [NodeJsToolchainService].
     *
     * Implementations are expected to declare a subtype with the parameters they need.
     */
    interface Parameters : BuildServiceParameters

    /**
     * Requests a Node.js distribution described by [nodeJsRequest].
     *
     * The returned provider is not evaluated eagerly - the distribution is only provisioned when the provider
     * value is queried, which for a task input happens after the configuration phase.
     */
    fun request(nodeJsRequest: NodeJsRequest): Provider<NodeJsExecutable>
}

/**
 * A description of the Node.js distribution a consumer needs.
 *
 * Instances are created by the Kotlin Gradle Plugin and read by a [NodeJsToolchainService] implementation.
 */
@ExperimentalNodeJsToolchainDsl
abstract class NodeJsRequest @Inject internal constructor() {

    /**
     * The requested Node.js version.
     *
     * There is intentionally no default value - the default version is defined per use case
     * (for example, resolving npm dependencies, or running tests).
     */
    abstract val version: Property<NodeJsVersion>

    /**
     * The requested operating system and architecture.
     *
     * Defaults to the platform of the machine running the build.
     */
    abstract val platform: Property<BuildPlatform>

    /**
     * Sets [version] from a raw Node.js version string, for example `24.16.0`.
     */
    fun version(version: String) {
        this.version.set(NodeJsVersion(version))
    }
}

/**
 * A provisioned Node.js installation.
 *
 * Can be used as a task input:
 * the [executable] itself is not tracked, while the [version] and the [platform] are,
 * so that up-to-date checks do not depend on the machine-specific installation path.
 */
@ExperimentalNodeJsToolchainDsl
abstract class NodeJsExecutable @Inject internal constructor() {

    /**
     * The Node.js executable.
     *
     * Either a full filesystem path to the executable, or just `node` when `node` from the `PATH` is used.
     */
    @get:Internal
    abstract val executable: Property<String>

    /**
     * The version of the provisioned Node.js distribution.
     */
    @get:Nested
    abstract val version: Property<NodeJsVersion>

    /**
     * The platform of the provisioned Node.js distribution.
     */
    @get:Nested
    abstract val platform: Property<BuildPlatform>
}

/**
 * A Node.js version, for example `24.16.0`.
 *
 * @param version the version string. Must not be blank.
 */
@ExperimentalNodeJsToolchainDsl
data class NodeJsVersion(
    @get:Input
    val version: String,
) : Serializable {
    init {
        require(version.isNotBlank()) { "Node.js version must not be blank" }
    }

    private companion object {
        private const val serialVersionUID: Long = 0L
    }
}

/**
 * An operating system and architecture pair, using the Node.js distribution naming,
 * for example `linux` and `x64`.
 *
 * @property os the operating system name, for example `linux`, `darwin` or `win`.
 * @property arch the CPU architecture, for example `x64` or `arm64`.
 */
@ExperimentalNodeJsToolchainDsl
data class BuildPlatform(
    @get:Input
    val os: String,
    @get:Input
    val arch: String,
) : Serializable {

    /**
     * Returns the platform in the Node.js distribution naming format `<os>-<arch>`, for example `linux-x64`.
     */
    override fun toString(): String = "$os-$arch"

    private companion object {
        private const val serialVersionUID: Long = 0L
    }
}
