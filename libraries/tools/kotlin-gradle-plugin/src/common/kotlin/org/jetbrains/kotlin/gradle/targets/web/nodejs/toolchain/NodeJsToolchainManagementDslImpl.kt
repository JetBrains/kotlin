/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.Action
import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.DefaultNodeJsToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsToolchainService
import javax.inject.Inject
import kotlin.reflect.KClass

@ExperimentalNodeJsToolchainDsl
@InternalKotlinGradlePluginApi
open class NodeJsToolchainManagementDslImpl @Inject constructor(
    private val gradle: Gradle,
) : NodeJsToolchainManagementDsl {

    private var configured = false

    override fun toolchainService(configure: DefaultNodeJsToolchainService.Parameters.() -> Unit) {
        toolchainService(DefaultNodeJsToolchainService::class) {
            offline.set(gradle.startParameter.isOffline)
            configure()
        }
    }

    @ExperimentalNodeJsToolchainDsl
    override fun toolchainService(configure: Action<DefaultNodeJsToolchainService.Parameters>) {
        toolchainService(DefaultNodeJsToolchainService::class) {
            offline.set(gradle.startParameter.isOffline)
            configure.execute(this)
        }
    }

    override fun <P : NodeJsToolchainService.Parameters, T : NodeJsToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: P.() -> Unit,
    ) {
        check(!configured) {
            "The Node.js toolchain service is already configured. " +
                    "Only one 'toolchainService(...)' or 'disable()' call is allowed inside 'toolchainManagement { nodeJs { ... } }'."
        }
        configured = true

        registerNodeJsToolchainServiceIfAbsent(gradle, serviceClass.java, configure)
    }
}
