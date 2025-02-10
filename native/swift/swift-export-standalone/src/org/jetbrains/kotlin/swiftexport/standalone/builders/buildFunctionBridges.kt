/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.bridge.*
import org.jetbrains.kotlin.sir.providers.source.KotlinPropertyAccessorOrigin
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.kotlinOriginOrNull
import org.jetbrains.kotlin.sir.providers.utils.isAbstract
import org.jetbrains.kotlin.sir.util.*
import org.jetbrains.kotlin.utils.addIfNotNull

internal fun SirFunction.constructFunctionBridgeRequests(generator: BridgeGenerator): List<FunctionBridgeRequest> {
    val fqName = ((origin as? KotlinSource)?.symbol as? KaFunctionSymbol)
        ?.callableId?.asSingleFqName()
        ?.pathSegments()?.map { it.toString() }
        ?: return emptyList()

    return listOfNotNull(
        patchCallableBodyAndGenerateRequest(generator, fqName)
    )
}

internal fun SirVariable.constructFunctionBridgeRequests(generator: BridgeGenerator): List<FunctionBridgeRequest> {
    val fqName = when (val origin = origin) {
        is KotlinSource -> (origin.symbol as? KaVariableSymbol)
            ?.callableId?.asSingleFqName()
            ?.pathSegments()?.map { it.toString() }
        is SirOrigin.ObjectAccessor -> ((origin.`for` as KotlinSource).symbol as KaNamedClassSymbol)
            .classId?.asSingleFqName()
            ?.pathSegments()?.map { it.toString() }
        else -> null
    } ?: return emptyList()

    val res = mutableListOf<FunctionBridgeRequest>()
    accessors.forEach {
        res.addIfNotNull(
            it.patchCallableBodyAndGenerateRequest(generator, fqName)
        )
    }

    return res.toList()
}

internal fun SirInit.constructFunctionBridgeRequests(generator: BridgeGenerator): List<FunctionBridgeRequest> {
    if (origin is SirOrigin.KotlinBaseInitOverride) {
        val names = parameters.map { it.argumentName!! }
        body = SirFunctionBody(buildList {
            add("super.init(${names.joinToString(separator = ", ") { "$it: $it" }})")
        })
        return emptyList()
    }

    val constructedClassSymbol = ((this.parent as? SirClass)?.origin as? KotlinSource)?.symbol as? KaClassSymbol
    if (constructedClassSymbol?.modality?.isAbstract() != false) {
        return emptyList()
    }

    val fqName = ((origin as? KotlinSource)?.symbol as? KaConstructorSymbol)
        ?.containingClassId?.asSingleFqName()
        ?.pathSegments()?.map { it.toString() }
        ?: return emptyList()

    return listOfNotNull(
        patchCallableBodyAndGenerateRequest(generator, fqName)
    )
}

internal fun SirFunction.constructPropertyAccessorsBridgeRequests(generator: BridgeGenerator): List<FunctionBridgeRequest> {
    val fqName = (origin as? KotlinPropertyAccessorOrigin)?.propertySymbol
        ?.callableId?.asSingleFqName()
        ?.pathSegments()?.map { it.toString() }
        ?: return emptyList()

    return listOfNotNull(
        patchCallableBodyAndGenerateRequest(generator, fqName)
    )
}

private fun SirCallable.patchCallableBodyAndGenerateRequest(
    generator: BridgeGenerator,
    fqName: List<String>,
): FunctionBridgeRequest? {
    val typesUsed = listOf(returnType) + allParameters.map { it.type }
    if (typesUsed.any { !it.isSupported })
        return null
    if (allParameters.any { it.type.isNever })
        return null // If any of the parameters is never - there should be no ability to call this function - therefor we can skip the bridge generation
    val suffix = bridgeSuffix
    val request = FunctionBridgeRequest(
        this,
        fqName.forBridge.joinToString("_") + suffix,
        fqName,
    )
    body = generator.generateSirFunctionBody(request)
    return request
}

private val SirType.isSupported: Boolean
    get() = when (this) {
        is SirNominalType -> {
            val declarationSupported = when (val declaration = typeDeclaration) {
                is SirTypealias -> declaration.type.isSupported
                else -> true
            }
            declarationSupported && typeArguments.all { it.isSupported }
        }
        is SirFunctionalType -> returnType.isSupported && parameterTypes.all { it.isSupported }
        is SirExistentialType -> true
        else -> false
    }

private val SirCallable.bridgeSuffix: String
    get() = when (this) {
        is SirAccessor -> "_$bridgeSuffix"
        is SirInit -> "_init"
        else -> when (kotlinOriginOrNull<KaFunctionSymbol>()) {
            is KaPropertyGetterSymbol -> "_get"
            is KaPropertySetterSymbol -> "_set"
            else -> ""
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
