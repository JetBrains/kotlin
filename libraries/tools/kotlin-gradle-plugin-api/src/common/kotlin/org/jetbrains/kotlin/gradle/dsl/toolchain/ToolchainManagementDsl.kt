/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import kotlin.reflect.KClass

/**
 * DSL for selecting and configuring the [KotlinToolchainService] of a single tool used by the whole build,
 * exposed as `toolchainManagement { <tool> { ... } }` in `settings.gradle.kts`
 * by the `org.jetbrains.kotlin.ecosystem` plugin.
 *
 * At most one of [toolchainService] or [disable] may be called - the [KotlinToolchainService] is registered as a
 * shared [org.gradle.api.services.BuildService] right away, under the same name every Kotlin Gradle Plugin project
 * looks up when it registers the toolchain service for itself, so calling this more than once would silently keep
 * only the first registration.
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 *
 * @since 2.5.20
 */
@ExperimentalKotlinToolchainDsl
interface ToolchainManagementDsl {

    /**
     * Configures the built-in, download-based toolchain service of the tool.
     *
     * This is the service used by the build when neither [toolchainService] nor [disable] is called.
     */
    @ExperimentalKotlinToolchainDsl
    fun toolchainService(configure: DownloadedToolchainParameters.() -> Unit)

    /**
     * [Action] based version of [toolchainService] above.
     */
    @ExperimentalKotlinToolchainDsl
    fun toolchainService(configure: Action<DownloadedToolchainParameters>) {
        toolchainService {
            configure.execute(this)
        }
    }

    /**
     * Selects [serviceClass] as the [KotlinToolchainService] of the tool used by the whole build,
     * and configures its [parameters][KotlinToolchainService.Parameters].
     */
    @ExperimentalKotlinToolchainDsl
    fun <P : KotlinToolchainService.Parameters, T : KotlinToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: P.() -> Unit,
    )

    /**
     * [Action] based version of [toolchainService] above.
     */
    @ExperimentalKotlinToolchainDsl
    fun <P : KotlinToolchainService.Parameters, T : KotlinToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: Action<P>,
    ) {
        toolchainService(serviceClass) {
            configure.execute(this)
        }
    }

    /**
     * Disables toolchain provisioning of the tool for the whole build.
     *
     * The tool is then configured and provisioned per project, using the environment spec and the setup task
     * of the tool, which is the behaviour of the Kotlin Gradle Plugin before the toolchain services were introduced.
     */
    @ExperimentalKotlinToolchainDsl
    fun disable()
}
