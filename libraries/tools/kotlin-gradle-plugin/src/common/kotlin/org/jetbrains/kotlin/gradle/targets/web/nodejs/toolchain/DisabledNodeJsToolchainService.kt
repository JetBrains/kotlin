/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.Project
import org.gradle.api.provider.Provider

abstract class DisabledNodeJsToolchainService: NodeJsToolchainService<NodeJsToolchainService.Parameters> {

    override fun request(configure: NodeJsRequest.() -> Unit): Provider<NodeJsExecutable> {
        throw UnsupportedOperationException("Node.js toolchain is disabled")
    }

    companion object {
        internal fun registerIfAbsent(project: Project): Provider<out NodeJsToolchainService<out NodeJsToolchainService.Parameters>> {
            return project.gradle.sharedServices.registerIfAbsent(nodeJsServiceName, DisabledNodeJsToolchainService::class.java)
        }
    }
}
