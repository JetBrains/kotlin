/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.runner.builders

import org.jetbrains.kotlin.sir.SirElement
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirKotlinOrigin
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.bridge.BridgeRequest
import org.jetbrains.kotlin.sir.bridge.createFunctionBodyFromRequest
import org.jetbrains.kotlin.sir.visitors.SirVisitorVoid
import org.jetbrains.sir.passes.SirPass
import org.jetbrains.sir.passes.run

internal fun SirModule.buildFunctionBridges(): List<BridgeRequest> {
    return BridgeGenerationPass.run(this)
}

private object BridgeGenerationPass : SirPass<SirElement, Nothing?, List<BridgeRequest>> {
    override fun run(element: SirElement, data: Nothing?): List<BridgeRequest> {
        val requests = mutableListOf<BridgeRequest>()
        element.accept(Visitor(requests))
        return requests.toList()
    }

    private class Visitor(val requests: MutableList<BridgeRequest>) : SirVisitorVoid() {

        override fun visitElement(element: SirElement) {
            element.acceptChildren(this)
        }

        override fun visitFunction(function: SirFunction) {
            val fqName = (function.origin as? SirKotlinOrigin.Function)?.path
                ?: return
            val fqNameForBridge = if (fqName.count() == 1) {
                listOf("__root__", fqName.first()) // todo: should be changed with correct mangling KT-64970
            } else {
                fqName
            }
            val bridgeRequest = BridgeRequest(
                function,
                fqNameForBridge.joinToString("_"),
                fqName
            )
            requests += bridgeRequest
            function.body = createFunctionBodyFromRequest(bridgeRequest)
        }
    }
}
