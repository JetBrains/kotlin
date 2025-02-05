/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.bridge.BridgeGenerator
import org.jetbrains.kotlin.sir.bridge.BridgeRequest

internal fun buildBridgeRequests(generator: BridgeGenerator, container: SirDeclarationContainer): List<BridgeRequest> = buildList {
    addAll(
        container
            .allCallables()
            .filterIsInstance<SirInit>()
            .flatMap { it.constructFunctionBridgeRequests(generator) }
    )
    addAll(
        container
            .allCallables()
            .filterIsInstance<SirFunction>()
            .flatMap { it.constructFunctionBridgeRequests(generator) + it.constructPropertyAccessorsBridgeRequests(generator) }
    )
    addAll(
        container
            .allVariables()
            .flatMap { it.constructFunctionBridgeRequests(generator) }
    )
    addAll(
        container
            .allClasses()
            .flatMap { it.constructTypeBindingBridgeRequests(generator) }
    )
    addAll(
        container
            .allContainers()
            .flatMap { buildBridgeRequests(generator, it) }
    )
}