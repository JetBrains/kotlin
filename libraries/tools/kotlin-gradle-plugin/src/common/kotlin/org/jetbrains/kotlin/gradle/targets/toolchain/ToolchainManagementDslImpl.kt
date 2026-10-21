/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.toolchain.DownloadedToolchainParameters
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.KotlinToolchainService
import kotlin.reflect.KClass

/**
 * The implementation of the `toolchainManagement { <tool> { ... } }` DSL shared by all tools.
 *
 * @suppress
 */
@InternalKotlinGradlePluginApi
open class ToolchainManagementDslImpl<S : KotlinToolchainService<*>> internal constructor(
    private val gradle: Gradle,
    private val dslBlockName: String,
    private val registrar: KotlinToolchainServiceRegistrar<S>,
    private val downloadedServiceClass: Class<out S>,
    private val disabledServiceClass: Class<out S>,
) : ToolchainManagementDsl {

    private var configured = false

    override fun toolchainService(configure: DownloadedToolchainParameters.() -> Unit) {
        registerOnce(downloadedServiceClass) { (it as DownloadedToolchainParameters).configure() }
    }

    override fun <P : KotlinToolchainService.Parameters, T : KotlinToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: P.() -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        registerOnce(serviceClass.java) { (it as P).configure() }
    }

    override fun disable() {
        registerOnce(disabledServiceClass) { }
    }

    private fun registerOnce(
        serviceClass: Class<out KotlinToolchainService<*>>,
        configure: (KotlinToolchainService.Parameters) -> Unit,
    ) {
        check(!configured) {
            "The $dslBlockName toolchain service is already configured. " +
                    "Only one 'toolchainService(...)' or 'disable()' call is allowed inside " +
                    "'toolchainManagement { $dslBlockName { ... } }'."
        }
        configured = true

        registrar.registerIfAbsent(gradle, serviceClass, configure)
    }
}
