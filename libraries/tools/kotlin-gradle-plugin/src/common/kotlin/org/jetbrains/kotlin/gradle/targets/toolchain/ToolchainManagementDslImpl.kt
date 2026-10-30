/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.DownloadedToolchainParameters
import org.jetbrains.kotlin.gradle.dsl.toolchain.KotlinToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainManagementDsl
import kotlin.reflect.KClass

/**
 * The implementation of the `toolchainManagement { <tool> { ... } }` DSL shared by all tools.
 *
 * The tool-specific DSL classes delegate to it.
 *
 * @param dslName the name of the `toolchainManagement { <dslName> { ... } }` block.
 * @param defaultServiceType the service selected by `toolchainService { ... }` without a class.
 * @param disabledServiceType the service selected by `disable()`.
 * @param register registers the selected service class for the build, with the configured parameters.
 */
internal class ToolchainManagementDslImpl(
    private val dslName: String,
    private val defaultServiceType: Class<out KotlinToolchainService<*>>,
    private val disabledServiceType: Class<out KotlinToolchainService<*>>,
    private val register: (
        serviceClass: Class<out KotlinToolchainService<*>>,
        configure: (KotlinToolchainService.Parameters) -> Unit,
    ) -> Unit,
) : ToolchainManagementDsl {

    constructor(gradle: Gradle, toolchain: ExternalToolchainRegistration<*>) : this(
        dslName = toolchain.dslName,
        defaultServiceType = toolchain.downloaded.serviceType,
        disabledServiceType = toolchain.disabled.serviceType,
        register = { serviceClass, configure -> toolchain.registerIfAbsent(gradle, serviceClass, configure) },
    )

    private var configured = false

    override fun toolchainService(configure: DownloadedToolchainParameters.() -> Unit) {
        registerOnce(defaultServiceType) { (it as DownloadedToolchainParameters).configure() }
    }

    override fun <P : KotlinToolchainService.Parameters, T : KotlinToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: P.() -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        registerOnce(serviceClass.java) { (it as P).configure() }
    }

    override fun disable() {
        registerOnce(disabledServiceType) { }
    }

    private fun registerOnce(
        serviceClass: Class<out KotlinToolchainService<*>>,
        configure: (KotlinToolchainService.Parameters) -> Unit,
    ) {
        check(!configured) {
            "The $dslName toolchain service is already configured. " +
                    "Only one 'toolchainService(...)' or 'disable()' call is allowed inside " +
                    "'toolchainManagement { $dslName { ... } }'."
        }
        configured = true

        register(serviceClass, configure)
    }
}
