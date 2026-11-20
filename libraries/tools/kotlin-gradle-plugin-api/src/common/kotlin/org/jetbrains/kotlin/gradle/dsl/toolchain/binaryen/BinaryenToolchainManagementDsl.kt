/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen

import kotlin.reflect.KClass
import org.gradle.api.Action
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl

/**
 * `binaryen { ... }` block of the `toolchainManagement { ... }` DSL.
 *
 * ```
 * // settings.gradle.kts
 * kotlin {
 *     toolchainManagement {
 *         binaryen {
 *             toolchainService(PreInstalledBinaryenToolchainService::class) {
 *                 toolchainDir.set(file("/opt/binaryen"))
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 *
 * @since 2.5.20
 */
@ExperimentalExternalToolchainDsl
interface BinaryenToolchainManagementDsl {
    /** Configures the built-in download-based Binaryen service. */
    fun toolchainService(configure: DefaultBinaryenToolchainService.Parameters.() -> Unit)

    /** [Action] version of [toolchainService]. */
    fun toolchainService(configure: Action<DefaultBinaryenToolchainService.Parameters>) {
        toolchainService { configure.execute(this) }
    }

    /** Selects a built-in or user-defined Binaryen service and configures its parameters. */
    fun <P : BuildServiceParameters, T : BinaryenToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: P.() -> Unit,
    )

    /** [Action] version of [toolchainService]. */
    fun <P : BuildServiceParameters, T : BinaryenToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: Action<P>,
    ) {
        toolchainService(serviceClass) { configure.execute(this) }
    }

    /** Uses the legacy per-project Binaryen setup instead of the build-wide toolchain service. */
    fun disable()
}
