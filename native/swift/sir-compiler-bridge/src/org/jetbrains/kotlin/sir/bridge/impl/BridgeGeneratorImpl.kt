/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge.impl

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.bridge.*
import org.jetbrains.kotlin.sir.util.*

private const val exportAnnotationFqName = "kotlin.native.internal.ExportedBridge"
private const val stdintHeader = "stdint.h"

internal class BridgeGeneratorImpl : BridgeGenerator {
    override fun generate(request: BridgeRequest): FunctionBridge {
        val returnTypeBridge = bridgeType(request.callable.returnType)
        val parameterBridges = request.callable.allParameters.mapIndexed { index, value -> bridgeParameter(value, index) }

        val cDeclaration = request.createCDeclaration()
        val kotlinBridge = createKotlinBridge(
            bridgeName = request.bridgeName,
            cName = request.cDeclarationName(),
            functionFqName = request.fqName,
            returnTypeBridge = returnTypeBridge,
            parameterBridges = parameterBridges
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
    val bridgeParameters = bridgeParameters()
    val nameSuffixForOverloadSimulation = bridgeParameters.joinToString(separator = "_", transform = { it.bridge.cType.repr })
    val suffixString = if (bridgeParameters.isNotEmpty()) "__TypesOfArguments__${nameSuffixForOverloadSimulation}__" else ""
    val result = "${bridgeName}${suffixString}"
    return result
}

private fun createKotlinBridge(
    bridgeName: String,
    cName: String,
    functionFqName: List<String>,
    returnTypeBridge: Bridge,
    parameterBridges: List<BridgeParameter>,
): List<String> {
    val declaration = createKotlinDeclarationSignature(bridgeName, returnTypeBridge, parameterBridges)
    val annotation = "@${exportAnnotationFqName.substringAfterLast('.')}(\"${cName}\")"

    val callSite = run {
        val functionName = functionFqName.joinToString(separator = ".")
        val parameters = parameterBridges.joinToString(", ") { "__${it.name}" }
        "$functionName($parameters)"
    }

    val parameters = parameterBridges.map { "val __${it.name} = ${it.bridge.generateSwiftToKotlinConversion(it.name)}" }

    val resultName = "_result"
    val result = returnTypeBridge.generateKotlinToSwiftConversion(resultName)

    return """
        $annotation
        $declaration {${parameters.takeIf { it.isNotEmpty() }?.joinToString(separator = "") { "\n            $it" } ?: ""}
            val $resultName = $callSite
            return $result
        }
    """.trimIndent().lines()
}

private fun createCallSite(functionFqName: List<String>, parameterNames: List<String>, resultName: String): String {
    val functionCall = "${functionFqName.joinToString(separator = ".")}(${parameterNames.joinToString(", ")})"
    return "val $resultName = $functionCall"
}

private fun createKotlinDeclarationSignature(bridgeName: String, returnTypeBridge: Bridge, parameters: List<BridgeParameter>): String {
    return "public fun $bridgeName(${
        parameters.joinToString(
            separator = ", ",
            transform = { "${it.name}: ${it.bridge.kotlinType.repr}" }
        )
    }): ${returnTypeBridge.kotlinType.repr}"
}

private fun BridgeRequest.createCDeclaration(): List<String> {
    val cParameters = bridgeParameters().joinToString(separator = ", ", transform = { "${it.bridge.cType.repr} ${it.name}" })
    val declaration = "${bridgeType(callable.returnType).cType.repr} ${cDeclarationName()}($cParameters);"
    return listOf(declaration)
}

private fun BridgeRequest.bridgeParameters() = callable.allParameters
    .mapIndexed { index, value -> bridgeParameter(value, index) }

private fun bridgeType(type: SirType): Bridge {
    require(type is SirNominalType)

    return when (type.type) {
        SirSwiftModule.void -> Bridge.AsIs(type,KotlinType.Unit, CType.Void)

        SirSwiftModule.bool -> Bridge.AsIs(type, KotlinType.Boolean, CType.Bool)

        SirSwiftModule.int8 -> Bridge.AsIs(type, KotlinType.Byte, CType.Int8)
        SirSwiftModule.int16 -> Bridge.AsIs(type, KotlinType.Short, CType.Int16)
        SirSwiftModule.int32 -> Bridge.AsIs(type, KotlinType.Int, CType.Int32)
        SirSwiftModule.int64 -> Bridge.AsIs(type, KotlinType.Long, CType.Int64)

        SirSwiftModule.uint8 -> Bridge.AsIs(type, KotlinType.UByte, CType.UInt8)
        SirSwiftModule.uint16 -> Bridge.AsIs(type, KotlinType.UShort, CType.UInt16)
        SirSwiftModule.uint32 -> Bridge.AsIs(type, KotlinType.UInt, CType.UInt32)
        SirSwiftModule.uint64 -> Bridge.AsIs(type, KotlinType.ULong, CType.UInt64)

        SirSwiftModule.double -> Bridge.AsIs(type, KotlinType.Double, CType.Double)
        SirSwiftModule.float -> Bridge.AsIs(type, KotlinType.Float, CType.Float)

        // TODO: Right now, we just assume everything nominal that we do not recognize is a class. We should make this decision looking at kotlin type?
        else -> Bridge.AsObject(type, KotlinType.Object, CType.UIntPtr)
    }
}

private fun bridgeParameter(parameter: SirParameter, index: Int): BridgeParameter {
    val bridgeParameterName = parameter.name?.let(::createBridgeParameterName) ?: "_$index"
    // TODO: Remove this check when non-trivial type bridges are supported
    check(!parameter.type.isVoid) { "The parameter $bridgeParameterName can not have Void type" }
    val bridge = bridgeType(parameter.type)
    return BridgeParameter(
        name = bridgeParameterName,
        bridge = bridge
    )
}

public fun createBridgeParameterName(kotlinName: String): String {
    // TODO: Post-process because C has stricter naming conventions.
    return kotlinName
}

internal data class BridgeParameter(
    val name: String,
    val bridge: Bridge,
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

    UIntPtr("uintptr_t"),
}

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

    Object(repr = "COpaquePointer")
}

internal sealed class Bridge(
    val swiftType: SirType,
    val kotlinType: KotlinType,
    val cType: CType,
) {
    class AsIs(swiftType: SirType, kotlinType: KotlinType, cType: CType) : Bridge(swiftType, kotlinType, cType) {
        override fun generateSwiftToKotlinConversion(parameterName: String): String = parameterName

        override fun generateKotlinToSwiftConversion(parameterName: String): String = parameterName
    }

    class AsObject(swiftType: SirType, kotlinType: KotlinType, cType: CType) : Bridge(swiftType, kotlinType, cType) {
        override fun generateSwiftToKotlinConversion(parameterName: String): String = "dereferenceSpecialRef($parameterName)"

        override fun generateKotlinToSwiftConversion(parameterName: String): String = "createSpecialRef($parameterName)"
    }

    abstract fun generateSwiftToKotlinConversion(parameterName: String): String

    abstract fun generateKotlinToSwiftConversion(parameterName: String): String
}