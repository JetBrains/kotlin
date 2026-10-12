/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter

import org.gradle.api.file.RegularFileProperty
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
 * Provisions WASI preview1 adapter modules for the Kotlin Gradle Plugin.
 *
 * The service accepts a [WasiAdapterRequest] to provision a [WasiAdapter] module.
 *
 * An implementation decides entirely how to satisfy the request: download the adapter,
 * reuse an already existing installation, or use an explicitly configured adapter file.
 *
 * The service is realized as a shared [BuildService], so a single instance is used by the whole build,
 * and provisioning is safe with Gradle Isolated Projects, the configuration cache, and parallel execution.
 *
 * This is a part of the Kotlin Gradle Plugin public API - external users may provide their own implementation.
 * The Kotlin Gradle Plugin ships the following built-in implementations:
 * - [DefaultWasiAdapterToolchainService] - downloads official WASI preview1 adapters from GitHub releases.
 * - [PreInstalledWasiAdapterToolchainService] - uses an explicitly configured adapter file.
 * - [DisabledWasiAdapterToolchainService] - disables WASI adapter provisioning.
 *
 * @param P the type of the build service parameters. Subtypes may add their own parameters.
 */
@ExperimentalWasmDsl
interface WasiAdapterToolchainService<P : WasiAdapterToolchainService.Parameters> : BuildService<P> {

    /**
     * Parameters of a [WasiAdapterToolchainService].
     *
     * Implementations are expected to declare a subtype with the parameters they need.
     */
    interface Parameters : BuildServiceParameters

    /**
     * Requests a WASI preview1 adapter module described by [adapterRequest].
     *
     * The returned provider is not evaluated eagerly - the adapter is only provisioned when the provider
     * value is queried, which for a task input happens after the configuration phase.
     */
    fun requestAdapter(adapterRequest: WasiAdapterRequest): Provider<WasiAdapter>
}

/**
 * A description of the WASI preview1 adapter module a consumer needs.
 *
 * Instances are created by the Kotlin Gradle Plugin and read by a [WasiAdapterToolchainService] implementation.
 */
@ExperimentalWasmDsl
abstract class WasiAdapterRequest @Inject internal constructor() {

    /**
     * The requested Wasmtime version for the adapter.
     */
    @get:Input
    abstract val version: Property<WasiAdapterVersion>

    /**
     * The requested adapter flavor.
     *
     * Defaults to [WasiAdapterFlavor.COMMAND].
     */
    @get:Input
    abstract val flavor: Property<WasiAdapterFlavor>

    /**
     * Sets [version] from a raw version string, for example `49.0.1`.
     */
    fun version(version: String) {
        this.version.set(WasiAdapterVersion(version))
    }
}

/**
 * A provisioned WASI preview1 adapter module.
 *
 * Can be used as a task input:
 * the [adapterFile] itself is not tracked, while the [version] and the [flavor] are,
 * so that up-to-date checks do not depend on the machine-specific installation path.
 */
@ExperimentalWasmDsl
abstract class WasiAdapter @Inject internal constructor() {

    /**
     * Absolute path of the provisioned adapter module file.
     */
    @get:Internal
    abstract val adapterFile: RegularFileProperty

    /**
     * The version of the provisioned adapter module.
     */
    @get:Nested
    abstract val version: Property<WasiAdapterVersion>

    /**
     * The flavor of the provisioned adapter module.
     */
    @get:Input
    abstract val flavor: Property<WasiAdapterFlavor>
}

/**
 * A Wasmtime release version the adapter is published with, for example `49.0.1`.
 *
 * @param version the version string. Must not be blank.
 */
@ExperimentalWasmDsl
class WasiAdapterVersion(
    @get:Input
    val version: String,
) : Serializable {
    init {
        require(version.isNotBlank()) { "WASI adapter version must not be blank" }
    }

    /**
     * Returns the string representation of the WASI adapter version.
     *
     * @return the version string of the WASI adapter.
     */
    override fun toString(): String = version
}

/**
 * The flavor of the WASI preview1 adapter module.
 */
@ExperimentalWasmDsl
enum class WasiAdapterFlavor {
    COMMAND,
    REACTOR,
    PROXY,
}
