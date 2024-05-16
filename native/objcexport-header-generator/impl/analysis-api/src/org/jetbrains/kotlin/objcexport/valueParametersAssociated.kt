package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.backend.konan.objcexport.MethodBridge
import org.jetbrains.kotlin.backend.konan.objcexport.MethodBridgeValueParameter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * This method is tightly bound to [getFunctionMethodBridge]
 *
 * See K1 implementation [org.jetbrains.kotlin.backend.konan.objcexport.MethodBrideExtensionsKt.valueParametersAssociated]
 */
context(KtAnalysisSession, KtObjCExportSession)
internal fun MethodBridge.valueParametersAssociated(
    function: KtFunctionLikeSymbol,
): List<Pair<MethodBridgeValueParameter, KtObjCParameterData?>> {

    val result = mutableListOf<Pair<MethodBridgeValueParameter, KtObjCParameterData?>>()
    val functionParameters = function.valueParameters
    val bridgeParameters = this.valueParameters

    result.addReceiver(bridgeParameters, function)

    if (function is KtPropertySetterSymbol) {
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

private fun MethodBridge.mapParameters(
    valueParameters: List<KtValueParameterSymbol>,
): List<Pair<MethodBridgeValueParameter, KtObjCParameterData?>> {
    return this.valueParameters.mapIndexed() { index, valueParameterBridge ->
        mapBridgeToFunctionParameters(valueParameterBridge, valueParameters.elementAtOrNull(index))
    }.filterNotNull()
}

context(KtAnalysisSession, KtObjCExportSession)
private fun MutableList<Pair<MethodBridgeValueParameter, KtObjCParameterData?>>.addReceiver(
    parameters: List<MethodBridgeValueParameter>,
    function: KtFunctionLikeSymbol,
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

private fun mapBridgeToFunctionParameters(
    bridgeParameter: MethodBridgeValueParameter?,
    functionParameter: KtValueParameterSymbol?,
): Pair<MethodBridgeValueParameter, KtObjCParameterData?>? {
    return if (bridgeParameter == null) null
    else if (functionParameter != null && bridgeParameter is MethodBridgeValueParameter.Mapped) bridgeParameter to KtObjCParameterData(
        name = functionParameter.name,
        isVararg = functionParameter.isVararg,
        type = functionParameter.returnType,
        isReceiver = false
    ) else bridgeParameter to null
}

internal data class KtObjCParameterData(
    val name: Name,
    val isVararg: Boolean,
    val type: KtType,
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
context(KtAnalysisSession)
internal val KtFunctionLikeSymbol.objCReceiverType: KtType?
    get() {
        return if (isConstructor) {
            /**
             * Edge case for supporting inner classes parameter.
             * See details at KT-66339
             */
            @Suppress("DEPRECATION")
            getDispatchReceiverType()
        } else if (isExtension) {
            if (receiverParameter?.type?.isMappedObjCType == true) receiverParameter?.type
            else if ((getContainingSymbol() as? KtNamedClassOrObjectSymbol)?.isInner == true) receiverParameter?.type
            else if (receiverParameter?.type?.isObjCNothing == true) return receiverParameter?.type
            else null
        } else if (this is KtPropertyGetterSymbol || this is KtPropertySetterSymbol) {
            val property = this.getContainingSymbol() as KtPropertySymbol
            val isExtension = property.isExtension
            val isInner = (property.getContainingSymbol() as? KtNamedClassOrObjectSymbol)?.isInner == true
            val receiverType = property.receiverType
            if (isExtension) {
                if (isInner) {
                    receiverType
                } else {
                    if (receiverType?.isMappedObjCType == true) receiverType else null
                }
            } else null

        } else null
    }
