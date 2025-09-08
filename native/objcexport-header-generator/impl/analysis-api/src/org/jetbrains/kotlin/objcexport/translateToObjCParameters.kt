/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.objcexport.analysisApiUtils.errorParameterName
import org.jetbrains.kotlin.objcexport.extras.objCTypeExtras
import org.jetbrains.kotlin.objcexport.extras.*
import org.jetbrains.kotlin.objcexport.mangling.unifyName

internal fun ObjCExportContext.translateToObjCParameters(symbol: KaFunctionSymbol, baseMethodBridge: MethodBridge): List<ObjCParameter> {
    val valueParametersAssociated = valueParametersAssociated(baseMethodBridge, symbol)
    val parameters = mutableListOf<ObjCParameter>()
    val usedNames = mutableSetOf<String>()

    valueParametersAssociated.forEach { (bridge: MethodBridgeValueParameter, parameter: KtObjCParameterData?) ->
        val candidateName = when (bridge) {
            is MethodBridgeValueParameter.Mapped -> {
                if (parameter == null) return@forEach
                when {
                    symbol is KaPropertySetterSymbol -> {
                        if (parameter.isReceiver) "receiver"
                        else "value"
                    }
                    else -> {
                        if (parameter.isReceiver) "receiver"
                        else parameter.name.toString()
                    }
                }
            }
            MethodBridgeValueParameter.ErrorOutParameter -> errorParameterName
            is MethodBridgeValueParameter.SuspendCompletion -> "completionHandler"
        }

        val uniqueName = unifyName(candidateName, usedNames)
        usedNames += uniqueName

        val type = when (bridge) {
            is MethodBridgeValueParameter.Mapped -> {
                val returnType = parameter!!.type
                if (parameter.isVararg) {
                    //vararg is a special case, [parameter.returnType] is T, we need Array<T>
                    val classType = analysisSession.buildClassType(StandardClassIds.Array) { argument(parameter.type) }
                    translateToObjCType(classType, bridge.bridge)
                } else {
                    translateToObjCType(returnType, bridge.bridge)
                }
            }
            MethodBridgeValueParameter.ErrorOutParameter ->
                ObjCPointerType(ObjCNullableReferenceType(ObjCClassType("NSError")), nullable = true)

            is MethodBridgeValueParameter.SuspendCompletion -> {
                val resultType = if (bridge.useUnitCompletion) {
                    null
                } else {
                    when (val type = translateToObjCReferenceType(symbol.returnType)) {
                        is ObjCNonNullReferenceType -> ObjCNullableReferenceType(type, isNullableResult = false)
                        is ObjCNullableReferenceType -> ObjCNullableReferenceType(type.nonNullType, isNullableResult = true)
                    }
                }
                ObjCBlockPointerType(
                    returnType = ObjCVoidType,
                    parameters = listOfNotNull(
                        resultType,
                        ObjCNullableReferenceType(ObjCClassType("NSError"))
                    ).map {
                        ObjCParameter("", null, it, todo = null)
                    }
                )
            }
        }

        val origin: ObjCExportStubOrigin? = null
        val todo: Nothing? = null
        parameters += ObjCParameter(uniqueName, origin, type, todo, objCTypeExtras {
            isErrorParameter = bridge is MethodBridgeValueParameter.ErrorOutParameter
        })
    }
    return parameters
}