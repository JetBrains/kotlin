/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain

import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl
import java.io.Serializable

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
@ExperimentalExternalToolchainDsl
class ExternalToolchainPlatform(
    @get:Input
    val os: String,
    @get:Input
    val arch: String,
) : Serializable {

    /** @suppress */
    override fun equals(other: Any?): Boolean =
        this === other || other is ExternalToolchainPlatform && os == other.os && arch == other.arch

    /** @suppress */
    override fun hashCode(): Int = 31 * os.hashCode() + arch.hashCode()

    /** Returns the platform in the `<os>-<arch>` format, for example `linux-x86_64`. */
    override fun toString(): String = "$os-$arch"

    private companion object {
        private const val serialVersionUID: Long = 0L
    }
}
