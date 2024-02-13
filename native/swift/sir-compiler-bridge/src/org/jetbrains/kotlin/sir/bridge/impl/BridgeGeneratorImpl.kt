/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge.impl

import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.bridge.*
import org.jetbrains.kotlin.sir.util.*

private const val exportAnnotationFqName = "kotlin.native.internal.ExportedBridge"
private const val stdintHeader = "stdint.h"

internal class BridgeGeneratorImpl : BridgeGenerator {
    override fun generate(request: BridgeRequest): FunctionBridge {
        val (kotlinReturnType, _) = bridgeType(request.callable.returnType)
        val parameterBridges = request.callable.allParameters.mapIndexed { index, value -> bridgeParameter(value, index) }

        val cDeclaration = request.createCDeclaration()
        val kotlinBridge = createKotlinBridge(
            bridgeName = request.bridgeName,
            cName = request.cDeclarationName(),
            functionFqName = request.fqName,
            returnType = kotlinReturnType,
            parameterBridges = parameterBridges.map { it.kotlin }
        )
        return FunctionBridge(
            KotlinFunctionBridge(kotlinBridge, listOf(exportAnnotationFqName)),
            CFunctionBridge(cDeclaration, listOf(stdintHeader))
        )
    }
}

// TODO: we need to mangle C name in more elegant way. KT-64970
// problems with this approach are:
// 1. there can be limit for declaration names in Clang compiler
// 1. this name will be UGLY in the debug session
internal fun BridgeRequest.cDeclarationName(): String {
    val nameSuffixForOverloadSimulation = cParameters().joinToString(separator = "_", transform = { it.type.repr })
    val suffixString = if (cParameters().isNotEmpty()) "__TypesOfArguments__${nameSuffixForOverloadSimulation}__" else ""
    val result = "${bridgeName}${suffixString}"
    return result
}

private fun createKotlinBridge(
    bridgeName: String,
    cName: String,
    functionFqName: List<String>,
    returnType: KotlinType,
    parameterBridges: List<KotlinBridgeParameter>,
): List<String> {
    val declaration = createKotlinDeclarationSignature(bridgeName, returnType, parameterBridges)
    val annotation = "@${exportAnnotationFqName.substringAfterLast('.')}(\"${cName}\")"
    val resultName = "result"
    val callSite = createCallSite(functionFqName, parameterBridges.map { it.name }, resultName)
    return """
        $annotation
        $declaration {
            $callSite
            return $resultName
        }
    """.trimIndent().lines()
}

private fun createCallSite(functionFqName: List<String>, parameterNames: List<String>, resultName: String): String {
    val functionCall = "${functionFqName.joinToString(separator = ".")}(${parameterNames.joinToString(", ")})"
    return "val $resultName = $functionCall"
}

private fun createKotlinDeclarationSignature(bridgeName: String, returnType: KotlinType, parameters: List<KotlinBridgeParameter>): String {
    return "public fun $bridgeName(${
        parameters.joinToString(
            separator = ", ",
            transform = { "${it.name}: ${it.type.repr}" }
        )
    }): ${returnType.repr}"
}

private fun BridgeRequest.createCDeclaration(): List<String> {
    val cParameters = cParameters().joinToString(separator = ", ", transform = { "${it.type.repr} ${it.name}" })
    val declaration = "${bridgeType(callable.returnType).second.repr} ${cDeclarationName()}($cParameters);"
    return listOf(declaration)
}

private fun BridgeRequest.cParameters() = callable.allParameters
    .mapIndexed { index, value -> bridgeParameter(value, index) }
    .map { it.c }

private fun bridgeType(type: SirType): Pair<KotlinType, CType> {
    require(type is SirNominalType)
    return when (type.type) {
        SirSwiftModule.void -> (KotlinType.Unit to CType.Void)

        SirSwiftModule.bool -> (KotlinType.Boolean to CType.Bool)

        SirSwiftModule.int8 -> (KotlinType.Byte to CType.Int8)
        SirSwiftModule.int16 -> (KotlinType.Short to CType.Int16)
        SirSwiftModule.int32 -> (KotlinType.Int to CType.Int32)
        SirSwiftModule.int64 -> (KotlinType.Long to CType.Int64)

        SirSwiftModule.uint8 -> (KotlinType.UByte to CType.UInt8)
        SirSwiftModule.uint16 -> (KotlinType.UShort to CType.UInt16)
        SirSwiftModule.uint32 -> (KotlinType.UInt to CType.UInt32)
        SirSwiftModule.uint64 -> (KotlinType.ULong to CType.UInt64)

        SirSwiftModule.double -> (KotlinType.Double to CType.Double)
        SirSwiftModule.float -> (KotlinType.Float to CType.Float)

        else -> error("Unsupported type: ${type.type.name}")
    }
}

private fun bridgeParameter(parameter: SirParameter, index: Int): BridgeParameter {
    val bridgeParameterName = parameter.name?.let(::createBridgeParameterName) ?: "_$index"
    // TODO: Remove this check when non-trivial type bridges are supported
    check(!parameter.type.isVoid) { "The parameter $bridgeParameterName can not have Void type" }
    val (kotlinType, cType) = bridgeType(parameter.type)
    return BridgeParameter(
        KotlinBridgeParameter(bridgeParameterName, kotlinType),
        CBridgeParameter(bridgeParameterName, cType)
    )
}

public fun createBridgeParameterName(kotlinName: String): String {
    // TODO: Post-process because C has stricter naming conventions.
    return kotlinName
}

internal data class BridgeParameter(
    val kotlin: KotlinBridgeParameter,
    val c: CBridgeParameter,
)

internal data class CBridgeParameter(
    val name: String,
    val type: CType,
)

public enum class CType(public val repr: String) {
    Void("void"),

    Bool("_Bool"),

    Int8("int8_t"),
    Int16("int16_t"),
    Int32("int32_t"),
    Int64("int64_t"),

    UInt8("uint8_t"),
    UInt16("uint16_t"),
    UInt32("uint32_t"),
    UInt64("uint64_t"),

    Float("float"),
    Double("double"),
}

internal data class KotlinBridgeParameter(
    val name: String,
    val type: KotlinType,
)

internal enum class KotlinType(val repr: String) {
    Unit("Unit"),

    Boolean("Boolean"),

    Byte("Byte"),
    Short("Short"),
    Int("Int"),
    Long("Long"),

    UByte("UByte"),
    UShort("UShort"),
    UInt("UInt"),
    ULong("ULong"),

    Float("Float"),
    Double("Double"),
}

