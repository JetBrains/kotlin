/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.objcexport.MethodBridge
import org.jetbrains.kotlin.backend.konan.objcexport.MethodBridgeValueParameter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * This method is tightly bound to [getFunctionMethodBridge]
 *
 * See K1 implementation [org.jetbrains.kotlin.backend.konan.objcexport.MethodBrideExtensionsKt.valueParametersAssociated]
 */
fun ObjCExportContext.valueParametersAssociated(
    bridge: MethodBridge,
    function: KaFunctionSymbol,
): List<Pair<MethodBridgeValueParameter, KtObjCParameterData?>> {
    exportSession.exportSessionValueParameters(function)?.let { return it }

    val result = mutableListOf<Pair<MethodBridgeValueParameter, KtObjCParameterData?>>()
    val functionParameters = function.valueParameters
    val bridgeParameters = bridge.valueParameters

    analysisSession.addReceiver(result, bridgeParameters, function)

    if (function is KaPropertySetterSymbol) {
        /**
         * We take second parameter from [bridgeParameters] because setter has always receiver and it's picked up with [addReceiver]
         */
        val bridgeParameter = bridgeParameters.toList().elementAtOrNull(1)
        val functionParameter = functionParameters.elementAtOrNull(0)
        result.addIfNotNull(exportSession.mapBridgeToFunctionParameters(bridgeParameter, functionParameter))
    } else {
        result.addAll(exportSession.mapParameters(bridge, functionParameters))
    }

    if (result.isEmpty() && bridgeParameters.isNotEmpty()) {
        result.addAll(exportSession.mapParameters(bridge, functionParameters))
    }

    return result
}

private fun KtObjCExportSession.mapParameters(
    bridge: MethodBridge,
    valueParameters: List<KaValueParameterSymbol>,
): List<Pair<MethodBridgeValueParameter, KtObjCParameterData?>> {
    return bridge.valueParameters.mapIndexed() { index, valueParameterBridge ->
        mapBridgeToFunctionParameters(valueParameterBridge, valueParameters.elementAtOrNull(index))
    }.filterNotNull()
}

private fun KaSession.addReceiver(
    list: MutableList<Pair<MethodBridgeValueParameter, KtObjCParameterData?>>,
    parameters: List<MethodBridgeValueParameter>,
    function: KaFunctionSymbol,
) {

    val receiverType = getObjCReceiverType(function)
    val receiverTypeName = receiverType?.expandedSymbol?.name

    if (receiverType != null && receiverTypeName != null) {
        list.add(
            parameters.first() to KtObjCParameterData(
                name = receiverTypeName,
                isVararg = false,
                type = receiverType,
                isReceiver = true
            )
        )
    }
}

private fun KtObjCExportSession.mapBridgeToFunctionParameters(
    bridgeParameter: MethodBridgeValueParameter?,
    functionParameter: KaValueParameterSymbol?,
): Pair<MethodBridgeValueParameter, KtObjCParameterData?>? {
    return if (bridgeParameter == null) null
    else if (functionParameter != null && bridgeParameter is MethodBridgeValueParameter.Mapped) bridgeParameter to KtObjCParameterData(
        name = Name.identifier(exportSessionSymbolName(functionParameter)),
        isVararg = functionParameter.isVararg,
        type = functionParameter.returnType,
        isReceiver = false
    ) else bridgeParameter to null
}

data class KtObjCParameterData(
    val name: Name,
    val isVararg: Boolean,
    val type: KaType,
    val isReceiver: Boolean,
)

/**
 * Not null in 4 cases:
 * 1. constructor of inner class
 * 2. function extension of inner class
 * 3. property extension of inner class
 * 4. function extension of [isMappedObjCType], i.e. fun String.foo()
 * 5. function extension of [Nothing], i.e. fun Nothing.foo()
 *
 * Members with non null [objCReceiverType] will have name `receiver`:
 * ```objective-c
 * - (int32_t)foo:(NSString *)receiver __attribute__((swift_name("foo(_:)")));
 * ```
 *
 * Also see [isObjCProperty]
 */
internal fun KaSession.getObjCReceiverType(symbol: KaFunctionSymbol?): KaType? {
    if (symbol == null) return null
    return if (symbol.isConstructor) {
        /**
         * Edge case for supporting inner classes parameter.
         * See details at KT-66339
         */
        @Suppress("DEPRECATION")
        symbol.dispatchReceiverType
    } else if (symbol.isExtension) {
        val receiverParameterType = symbol.receiverParameter?.type
        if (isMappedObjCType(receiverParameterType)) receiverParameterType
        else if ((symbol.containingDeclaration as? KaNamedClassSymbol)?.isInner == true) receiverParameterType
        else if (receiverParameterType != null && isObjCNothing(receiverParameterType)) return receiverParameterType
        else null
    } else if (symbol is KaPropertyGetterSymbol || symbol is KaPropertySetterSymbol) {
        val property = symbol.containingDeclaration as KaPropertySymbol
        val isExtension = property.isExtension
        val receiverType = property.receiverType
        if (isExtension) {
            if (getClassIfCategory(receiverType) == null) {
                receiverType
            } else {
                null
            }
        } else null

    } else null
}
