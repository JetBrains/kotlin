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
context(KaSession, KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun MethodBridge.valueParametersAssociated(
    function: KaFunctionSymbol,
): List<Pair<MethodBridgeValueParameter, KtObjCParameterData?>> {
    function.exportSessionValueParameters()?.let { return it }

    val result = mutableListOf<Pair<MethodBridgeValueParameter, KtObjCParameterData?>>()
    val functionParameters = function.valueParameters
    val bridgeParameters = this.valueParameters

    result.addReceiver(bridgeParameters, function)

    if (function is KaPropertySetterSymbol) {
        /**
         * We take second parameter from [bridgeParameters] because setter has always receiver and it's picked up with [addReceiver]
         */
        val bridgeParameter = bridgeParameters.toList().elementAtOrNull(1)
        val functionParameter = functionParameters.elementAtOrNull(0)
        result.addIfNotNull(mapBridgeToFunctionParameters(bridgeParameter, functionParameter))
    } else {
        result.addAll(mapParameters(functionParameters))
    }

    if (result.isEmpty() && bridgeParameters.isNotEmpty()) {
        result.addAll(mapParameters(functionParameters))
    }

    return result
}

context(KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun MethodBridge.mapParameters(
    valueParameters: List<KaValueParameterSymbol>,
): List<Pair<MethodBridgeValueParameter, KtObjCParameterData?>> {
    return this.valueParameters.mapIndexed() { index, valueParameterBridge ->
        mapBridgeToFunctionParameters(valueParameterBridge, valueParameters.elementAtOrNull(index))
    }.filterNotNull()
}

context(KaSession, KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun MutableList<Pair<MethodBridgeValueParameter, KtObjCParameterData?>>.addReceiver(
    parameters: List<MethodBridgeValueParameter>,
    function: KaFunctionSymbol,
) {

    val receiverType = function.objCReceiverType
    val receiverTypeName = receiverType?.expandedSymbol?.name

    if (receiverType != null && receiverTypeName != null) {
        add(
            parameters.first() to KtObjCParameterData(
                name = receiverTypeName,
                isVararg = false,
                type = receiverType,
                isReceiver = true
            )
        )
    }
}

context(KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun mapBridgeToFunctionParameters(
    bridgeParameter: MethodBridgeValueParameter?,
    functionParameter: KaValueParameterSymbol?,
): Pair<MethodBridgeValueParameter, KtObjCParameterData?>? {
    return if (bridgeParameter == null) null
    else if (functionParameter != null && bridgeParameter is MethodBridgeValueParameter.Mapped) bridgeParameter to KtObjCParameterData(
        name = Name.identifier(functionParameter.exportSessionSymbolName()),
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
context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal val KaFunctionSymbol.objCReceiverType: KaType?
    get() {
        return if (isConstructor) {
            /**
             * Edge case for supporting inner classes parameter.
             * See details at KT-66339
             */
            @Suppress("DEPRECATION")
            dispatchReceiverType
        } else if (isExtension) {
            if (receiverParameter?.type?.isMappedObjCType == true) receiverParameter?.type
            else if ((containingSymbol as? KaNamedClassSymbol)?.isInner == true) receiverParameter?.type
            else if (receiverParameter?.type?.isObjCNothing == true) return receiverParameter?.type
            else null
        } else if (this is KaPropertyGetterSymbol || this is KaPropertySetterSymbol) {
            val property = containingSymbol as KaPropertySymbol
            val isExtension = property.isExtension
            val receiverType = property.receiverType
            if (isExtension) {
                if (receiverType?.getClassIfCategory() == null) {
                    receiverType
                } else {
                    null
                }
            } else null

        } else null
    }
