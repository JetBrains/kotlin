/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.bridge.BridgeGenerator
import org.jetbrains.kotlin.sir.bridge.BridgeRequest
import org.jetbrains.kotlin.sir.providers.SirAndKaSession

internal fun SirAndKaSession.buildBridgeRequests(generator: BridgeGenerator, container: SirDeclarationContainer): List<BridgeRequest> = buildList {
        addAll(
            container
                .allCallables()
                .filterIsInstance<SirInit>()
                .flatMap { constructFunctionBridgeRequests(it, generator) }
        )
        addAll(
            container
                .allCallables()
                .filterIsInstance<SirFunction>()
                .flatMap { constructFunctionBridgeRequests(it, generator) + constructPropertyAccessorsBridgeRequests(it, generator) }
        )
        addAll(
            container
                .allVariables()
                .flatMap { constructFunctionBridgeRequests(it, generator) }
        )
        addAll(
            container
                .allClasses()
                .flatMap { it.constructTypeBindingBridgeRequests() }
        )
        addAll(
            container
                .allProtocols()
                .flatMap { it.constructTypeBindingBridgeRequests() }
        )
        addAll(
            container
                .allContainers()
                .flatMap { buildBridgeRequests(generator, it) }
        )
    }