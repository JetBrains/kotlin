/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.util.*
import org.jetbrains.kotlin.sir.bridge.BridgeRequest
import org.jetbrains.kotlin.sir.bridge.createFunctionBodyFromRequest
import org.jetbrains.kotlin.sir.visitors.SirVisitorVoid
import org.jetbrains.kotlin.utils.addIfNotNull
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

            requests.addIfNotNull(
                function.patchCallableBodyAndGenerateRequest(fqName)
            )
        }

        override fun visitVariable(variable: SirVariable) {
            val fqName = ((variable.origin as? KotlinSource)?.symbol as? KtVariableLikeSymbol)
                ?.callableIdIfNonLocal?.asSingleFqName()
                ?.pathSegments()?.map { it.toString() }
                ?: return

            variable.accessors.forEach {
                requests.addIfNotNull(
                    it.patchCallableBodyAndGenerateRequest(fqName)
                )
            }
        }
    }
}

private fun SirCallable.patchCallableBodyAndGenerateRequest(
    fqName: List<String>,
): BridgeRequest? = when (kind) {
    SirCallableKind.FUNCTION,
    SirCallableKind.STATIC_METHOD,
    -> {
        val suffix = bridgeSuffix
        val request = BridgeRequest(
            this,
            fqName.forBridge.joinToString("_") + suffix,
            fqName
        )
        body = createFunctionBodyFromRequest(request)
        request
    }
    SirCallableKind.INSTANCE_METHOD,
    SirCallableKind.CLASS_METHOD,
    -> {
        body = SirFunctionBody(listOf("fatalError()"))
        null
    }
}

private val SirCallable.bridgeSuffix: String
    get() = when (this) {
        is SirAccessor -> "_$bridgeSuffix"
        else -> ""
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
