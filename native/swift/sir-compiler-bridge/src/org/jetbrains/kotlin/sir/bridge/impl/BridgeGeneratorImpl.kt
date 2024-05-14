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
    override fun generateFunctionBridges(request: BridgeRequest) = buildList {
        when (request.callable) {
            is SirFunction -> {
                add(
                    request.descriptor.createFunctionBridge { args ->
                        "${request.fqName.joinToString(separator = ".")}(${args.joinToString()})"
                    }
                )
            }
            is SirGetter -> {
                add(
                    request.descriptor.createFunctionBridge { args ->
                        val name = request.fqName.joinToString(separator = ".")
                        require(args.isEmpty()) { "Received a getter $name with ${args.size} parameters instead of no parameters, aborting" }
                        name
                    }
                )
            }
            is SirSetter -> {
                add(
                    request.descriptor.createFunctionBridge { args ->
                        val name = request.fqName.joinToString(separator = ".")
                        require(args.size == 1) { "Received a setter $name with ${args.size} parameters instead of a single one, aborting" }
                        "$name = ${args.single()}"
                    }
                )
            }
            is SirInit -> {
                add(
                    request.allocationDescriptor.createFunctionBridge { args ->
                        val typeName = request.fqName.joinToString(separator = ".")
                        "kotlin.native.internal.createUninitializedInstance<$typeName>(${args.joinToString()})"
                    }
                )
                add(
                    request.initializationDescriptor.createFunctionBridge { args ->
                        val ctorName = request.fqName.joinToString(separator = ".")
                        "kotlin.native.internal.initInstance(${args.first()}, ${ctorName}(${args.drop(1).joinToString()}))"
                    }
                )
            }
        }
    }

    override fun generateSirFunctionBody(request: BridgeRequest) = SirFunctionBody(buildList {
        when (request.callable) {
            is SirFunction, is SirGetter, is SirSetter -> {
                add("return ${request.descriptor.swiftCall()}")
            }
            is SirInit -> {
                add("let ${obj.name} = ${request.allocationDescriptor.swiftCall()}")
                add("super.init(__externalRCRef: ${obj.name})")
                add(request.initializationDescriptor.swiftCall())
            }
        }
    })
}

private class BridgeFunctionDescriptor(
    val kotlinName: String,
    val parameters: List<BridgeParameter>,
    val returnType: Bridge,
) {
    val cName = cDeclarationName(kotlinName, parameters)
}

private val BridgeRequest.descriptor: BridgeFunctionDescriptor
    get() {
        require(callable !is SirInit) { "Use allocationDescriptor and initializationDescriptor instead" }
        return BridgeFunctionDescriptor(
            bridgeName,
            callable.bridgeParameters(),
            bridgeType(callable.returnType),
        )
    }

private val obj = BridgeParameter("__kt", bridgeType(SirNominalType(SirSwiftModule.uint)))

private val BridgeRequest.allocationDescriptor: BridgeFunctionDescriptor
    get() {
        require(callable is SirInit) { "Use descriptor instead" }
        return BridgeFunctionDescriptor(
            bridgeName + "_allocate",
            emptyList(),
            obj.bridge,
        )
    }

private val BridgeRequest.initializationDescriptor: BridgeFunctionDescriptor
    get() {
        require(callable is SirInit) { "Use descriptor instead" }
        return BridgeFunctionDescriptor(
            bridgeName + "_initialize",
            listOf(obj) + callable.bridgeParameters(),
            bridgeType(callable.returnType),
        )
    }

// TODO: we need to mangle C name in more elegant way. KT-64970
// problems with this approach are:
// 1. there can be limit for declaration names in Clang compiler
// 1. this name will be UGLY in the debug session
private fun cDeclarationName(bridgeName: String, parameterBridges: List<BridgeParameter>): String {
    val nameSuffixForOverloadSimulation = parameterBridges.joinToString(separator = "_", transform = { it.bridge.cType.repr })
    val suffixString = if (parameterBridges.isNotEmpty()) "__TypesOfArguments__${nameSuffixForOverloadSimulation}__" else ""
    val result = "${bridgeName}${suffixString}"
    return result
}

private inline fun BridgeFunctionDescriptor.createKotlinBridge(
    buildCallSite: (args: List<String>) -> String,
) = buildList {
    add("@${exportAnnotationFqName.substringAfterLast('.')}(\"${cName}\")")
    add("public fun $kotlinName(${parameters.joinToString { "${it.name}: ${it.bridge.kotlinType.repr}" }}): ${returnType.kotlinType.repr} {")
    val indent = "    "
    parameters.forEach {
        add("${indent}val __${it.name} = ${it.bridge.generateSwiftToKotlinConversion(it.name)}")
    }
    val callSite = buildCallSite(parameters.map { "__${it.name}" })
    if (returnType.swiftType.isVoid) {
        add("${indent}$callSite")
    } else {
        val resultName = "_result"
        add("${indent}val $resultName = $callSite")
        add("${indent}return ${returnType.generateKotlinToSwiftConversion(resultName)}")
    }
    add("}")
}

private fun BridgeFunctionDescriptor.swiftCall() = "$cName(${parameters.joinToString { parameter -> parameter.name }})"

private fun BridgeFunctionDescriptor.cDeclaration() =
    "${returnType.cType.repr} ${cName}(${parameters.joinToString { "${it.bridge.cType.repr} ${it.name}" }});"

private inline fun BridgeFunctionDescriptor.createFunctionBridge(kotlinCall: (args: List<String>) -> String) =
    FunctionBridge(
        KotlinFunctionBridge(createKotlinBridge(kotlinCall), listOf(exportAnnotationFqName)),
        CFunctionBridge(listOf(cDeclaration()), listOf(stdintHeader))
    )

private fun SirCallable.bridgeParameters() = allParameters
    .mapIndexed { index, value -> bridgeParameter(value, index) }

private fun bridgeType(type: SirType): Bridge {
    require(type is SirNominalType)

    return when (type.type) {
        SirSwiftModule.void -> Bridge.AsIs(type, KotlinType.Unit, CType.Void)

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

        SirSwiftModule.uint -> Bridge.AsObject(type, KotlinType.Object, CType.Object)

        // TODO: Right now, we just assume everything nominal that we do not recognize is a class. We should make this decision looking at kotlin type?
        else -> Bridge.AsObject(type, KotlinType.Object, CType.Object)
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

private fun createBridgeParameterName(kotlinName: String): String {
    // TODO: Post-process because C has stricter naming conventions.
    return kotlinName
}

private data class BridgeParameter(
    val name: String,
    val bridge: Bridge,
)

private enum class CType(public val repr: String) {
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

    Object("uintptr_t"),
}

private enum class KotlinType(val repr: String) {
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

    Object(repr = "kotlin.native.internal.NativePtr")
}

private sealed class Bridge(
    val swiftType: SirType,
    val kotlinType: KotlinType,
    val cType: CType,
) {
    class AsIs(swiftType: SirType, kotlinType: KotlinType, cType: CType) : Bridge(swiftType, kotlinType, cType) {
        override fun generateSwiftToKotlinConversion(parameterName: String): String = parameterName

        override fun generateKotlinToSwiftConversion(parameterName: String): String = parameterName
    }

    class AsObject(swiftType: SirType, kotlinType: KotlinType, cType: CType) : Bridge(swiftType, kotlinType, cType) {
        override fun generateSwiftToKotlinConversion(parameterName: String): String =
            "kotlin.native.internal.ref.dereferenceExternalRCRef($parameterName)"

        override fun generateKotlinToSwiftConversion(parameterName: String): String =
            "kotlin.native.internal.ref.createRetainedExternalRCRef($parameterName)"
    }

    abstract fun generateSwiftToKotlinConversion(parameterName: String): String

    abstract fun generateKotlinToSwiftConversion(parameterName: String): String
}
