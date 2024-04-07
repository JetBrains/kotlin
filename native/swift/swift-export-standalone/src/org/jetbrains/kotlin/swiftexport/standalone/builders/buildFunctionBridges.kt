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
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.utils.addIfNotNull

internal fun SirModule.buildFunctionBridges(): List<BridgeRequest> {
    return BridgeGenerationPass.run(this)
}

private object BridgeGenerationPass {
    fun run(container: SirDeclarationContainer): List<BridgeRequest> {
        val requests = mutableListOf<BridgeRequest>()

        requests.addAll(
            container
                .allCallables()
                .filterIsInstance<SirFunction>()
                .map { it.constructBridgeRequests() }
                .flatten()
        )

        requests.addAll(
            container
                .allVariables()
                .map { it.constructBridgeRequests() }
                .flatten()
        )

        requests.addAll(
            container
                .allContainers()
                .flatMap { run(it) }
        )

        return requests.toList()
    }

    private fun SirFunction.constructBridgeRequests(): List<BridgeRequest> {
        val fqName = ((origin as? KotlinSource)?.symbol as? KtFunctionLikeSymbol)
            ?.callableIdIfNonLocal?.asSingleFqName()
            ?.pathSegments()?.map { it.toString() }
            ?: return emptyList()

        return listOfNotNull(
            patchCallableBodyAndGenerateRequest(fqName)
        )
    }

    private fun SirVariable.constructBridgeRequests(): List<BridgeRequest> {
        val fqName = ((origin as? KotlinSource)?.symbol as? KtVariableLikeSymbol)
            ?.callableIdIfNonLocal?.asSingleFqName()
            ?.pathSegments()?.map { it.toString() }
            ?: return emptyList()

        val res = mutableListOf<BridgeRequest>()
        accessors.forEach {
            res.addIfNotNull(
                it.patchCallableBodyAndGenerateRequest(fqName)
            )
        }

        return res.toList()
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
