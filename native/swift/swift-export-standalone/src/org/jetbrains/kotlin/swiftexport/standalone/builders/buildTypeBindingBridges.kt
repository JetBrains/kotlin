/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.bridge.BridgeGenerator
import org.jetbrains.kotlin.sir.bridge.TypeBindingBridgeRequest
import org.jetbrains.kotlin.sir.providers.source.KotlinSource

internal fun SirClass.constructTypeBindingBridgeRequests(generator: BridgeGenerator): List<TypeBindingBridgeRequest> {
    // `SirClass` must be generated from Kotlin sources, and be a named class.
    (origin as? KotlinSource)?.symbol as? KaNamedClassSymbol ?: return emptyList()

    return listOf(TypeBindingBridgeRequest(this))
}