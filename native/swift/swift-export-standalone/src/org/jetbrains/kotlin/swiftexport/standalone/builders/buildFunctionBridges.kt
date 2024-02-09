/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.sir.SirElement
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirVariable
import org.jetbrains.kotlin.sir.SirAccessor
import org.jetbrains.kotlin.sir.SirSetter
import org.jetbrains.kotlin.sir.SirGetter
import org.jetbrains.kotlin.sir.util.*
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.bridge.BridgeRequest
import org.jetbrains.kotlin.sir.bridge.createFunctionBodyFromRequest
import org.jetbrains.kotlin.sir.visitors.SirVisitorVoid
import org.jetbrains.sir.passes.SirPass
import org.jetbrains.sir.passes.builder.KotlinSource
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
            val fqName = ((function.origin as? KotlinSource)?.symbol as? KtFunctionLikeSymbol)
                ?.callableIdIfNonLocal?.asSingleFqName()
                ?.pathSegments()?.map { it.toString() }
                ?: return
            val fqNameForBridge = fqName.forBridge

            val bridgeRequest = BridgeRequest(
                function,
                fqNameForBridge.joinToString("_"),
                fqName
            )
            requests += bridgeRequest
            function.body = createFunctionBodyFromRequest(bridgeRequest)
        }

        override fun visitVariable(variable: SirVariable) {
            val fqName = ((variable.origin as? KotlinSource)?.symbol as? KtVariableLikeSymbol)
                ?.callableIdIfNonLocal?.asSingleFqName()
                ?.pathSegments()?.map { it.toString() }
                ?: return
            val fqNameForBridge = fqName.forBridge

            variable.accessors.forEach {
                val suffix = it.bridgeSuffix
                val request = BridgeRequest(
                    it,
                    fqNameForBridge.joinToString("_") + "_$suffix",
                    fqName
                )
                requests += request
                it.body = createFunctionBodyFromRequest(request)
            }
        }
    }
}

private val SirAccessor.bridgeSuffix: String
    get() = when (this) {
        is SirGetter -> "get"
        is SirSetter -> "set"
    }

private val List<String>.forBridge: List<String>
    get() = if (this.count() == 1) {
        listOf("__root__", this.first()) // todo: should be changed with correct mangling KT-64970
    } else {
        this
    }
