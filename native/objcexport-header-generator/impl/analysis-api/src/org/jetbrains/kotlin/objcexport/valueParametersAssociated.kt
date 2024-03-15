package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.backend.konan.objcexport.MethodBridge
import org.jetbrains.kotlin.backend.konan.objcexport.MethodBridgeValueParameter
import org.jetbrains.kotlin.backend.konan.objcexport.ReferenceBridge
import org.jetbrains.kotlin.name.Name

/**
 * See K1 implementation [org.jetbrains.kotlin.backend.konan.objcexport.MethodBrideExtensionsKt.valueParametersAssociated]
 */
context(KtAnalysisSession)
internal fun MethodBridge.valueParametersAssociated(
    function: KtFunctionLikeSymbol,
): List<Pair<MethodBridgeValueParameter, KtObjCParameterData?>> {

    val result = mutableListOf<Pair<MethodBridgeValueParameter, KtObjCParameterData?>>()
    val valueParameters = function.valueParameters
    val receiverType = function.objCReceiverType
    val receiverTypeName = receiverType?.expandedClassSymbol?.name

    if (receiverType != null && receiverTypeName != null) {
        result.add(
            MethodBridgeValueParameter.Mapped(ReferenceBridge) to KtObjCParameterData(
                name = receiverTypeName,
                isVararg = false,
                type = receiverType,
                isReceiver = true
            )
        )
    }

    this.valueParameters.forEachIndexed { index, valueParameterBridge ->

        if (valueParameterBridge is MethodBridgeValueParameter.Mapped) {
            result.add(
                valueParameterBridge to KtObjCParameterData(
                    name = valueParameters[index].name,
                    isVararg = valueParameters[index].isVararg,
                    type = valueParameters[index].returnType,
                    isReceiver = false
                )
            )
        } else {
            result.add(valueParameterBridge to null)
        }
    }

    return result
}

internal data class KtObjCParameterData(
    val name: Name,
    val isVararg: Boolean,
    val type: KtType,
    val isReceiver: Boolean,
)

/**
 * Not null in 3 cases:
 * 1. constructor of inner class
 * 2. function extension of inner class
 * 3. property extension of inner class
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
            if ((getContainingSymbol() as? KtNamedClassOrObjectSymbol)?.isInner == true) receiverParameter?.type
            else null
        } else if (this is KtPropertyGetterSymbol || this is KtPropertySetterSymbol) {
            val property = this.getContainingSymbol() as KtPropertySymbol
            val isExtension = property.isExtension
            val isInner = (property.getContainingSymbol() as? KtNamedClassOrObjectSymbol)?.isInner == true
            if (isExtension && isInner) property.receiverType else null
        } else {
            null
        }
    }
