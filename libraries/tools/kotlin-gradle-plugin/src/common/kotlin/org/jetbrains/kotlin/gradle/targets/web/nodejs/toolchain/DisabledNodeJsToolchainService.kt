/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainService.Companion.nodeJsServiceName

abstract class DisabledNodeJsToolchainService : NodeJsToolchainService<NodeJsToolchainService.Parameters> {

    override fun request(nodeJsRequest: NodeJsRequest): Provider<NodeJsExecutable> {
        throw UnsupportedOperationException("Node.js toolchain is disabled")
    }

    companion object {
        internal fun registerIfAbsent(gradle: Gradle): Provider<out NodeJsToolchainService<out NodeJsToolchainService.Parameters>> {
            return gradle.sharedServices.registerIfAbsent(nodeJsServiceName, DisabledNodeJsToolchainService::class.java)
        }
    }
}
