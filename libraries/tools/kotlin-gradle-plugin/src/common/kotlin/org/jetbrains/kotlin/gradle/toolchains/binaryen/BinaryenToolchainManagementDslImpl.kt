/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExternalToolchainDsl::class)

package org.jetbrains.kotlin.gradle.toolchains.binaryen

import org.gradle.api.invocation.Gradle
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.BinaryenToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.BinaryenToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.DefaultBinaryenToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.DisabledBinaryenToolchainService
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * The `toolchainManagement { binaryen { ... } }` DSL.
 *
 * @suppress
 */
@InternalKotlinGradlePluginApi
open class BinaryenToolchainManagementDslImpl @Inject constructor(
    private val gradle: Gradle,
) : BinaryenToolchainManagementDsl {
    private var configured = false

    override fun toolchainService(configure: DefaultBinaryenToolchainService.Parameters.() -> Unit) {
        toolchainService(DefaultBinaryenToolchainService::class, configure)
    }

    override fun <P : BuildServiceParameters, T : BinaryenToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: P.() -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        registerOnce(serviceClass.java) { (it as P).configure() }
    }

    override fun disable() {
        registerOnce(DisabledBinaryenToolchainService::class.java) { }
    }

    private fun registerOnce(
        serviceClass: Class<out BinaryenToolchainService<*>>,
        configure: (BuildServiceParameters) -> Unit,
    ) {
        check(!configured) {
            "The binaryen toolchain service is already configured. " +
                    "Only one 'toolchainService(...)' or 'disable()' call is allowed inside " +
                    "'toolchainManagement { binaryen { ... } }'."
        }
        configured = true
        gradle.registerBinaryenToolchainService(serviceClass, configure)
    }
}
