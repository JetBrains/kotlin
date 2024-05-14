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

internal class BridgeGeneratorImpl(val typeNamer: SirTypeNamer) : BridgeGenerator {
    override fun generateFunctionBridges(request: BridgeRequest) = buildList {
        when (request.callable) {
            is SirFunction -> {
                add(
                    request.descriptor.createFunctionBridge(typeNamer) { name, args ->
                        "$name(${args.joinToString()})"
                    }
                )
            }
            is SirGetter -> {
                add(
                    request.descriptor.createFunctionBridge(typeNamer) { name, args ->
                        require(args.isEmpty()) { "Received a getter $name with ${args.size} parameters instead of no parameters, aborting" }
                        name
                    }
                )
            }
            is SirSetter -> {
                add(
                    request.descriptor.createFunctionBridge(typeNamer) { name, args ->
                        require(args.size == 1) { "Received a setter $name with ${args.size} parameters instead of a single one, aborting" }
                        "$name = ${args.single()}"
                    }
                )
            }
            is SirInit -> {
                add(
                    request.allocationDescriptor.createFunctionBridge(typeNamer) { name, args ->
                        "kotlin.native.internal.createUninitializedInstance<$name>(${args.joinToString()})"
                    }
                )
                add(
                    request.initializationDescriptor.createFunctionBridge(typeNamer) { name, args ->
                        "kotlin.native.internal.initInstance(${args.first()}, ${name}(${args.drop(1).joinToString()}))"
                    }
                )
            }
        }
    }

    override fun generateSirFunctionBody(request: BridgeRequest) = SirFunctionBody(buildList {
        when (request.callable) {
            is SirFunction, is SirGetter, is SirSetter -> {
                add("return ${request.descriptor.swiftCall(typeNamer)}")
            }
            is SirInit -> {
                add("let ${obj.name} = ${request.allocationDescriptor.swiftCall(typeNamer)}")
                add("super.init(__externalRCRef: ${obj.name})")
                add(request.initializationDescriptor.swiftCall(typeNamer))
            }
        }
    })
}

private class BridgeFunctionDescriptor(
    val kotlinBridgeName: String,
    val parameters: List<BridgeParameter>,
    val returnType: Bridge,
    val kotlinFqName: List<String>,
    val selfParameter: BridgeParameter?,
) {
    val cBridgeName = cDeclarationName(kotlinBridgeName, parameters)

    val allParameters
        get() = listOfNotNull(selfParameter) + parameters

    val kotlinName
        get() = if (selfParameter != null) {
            "__${selfParameter.name}.${kotlinFqName.last()}"
        } else {
            kotlinFqName.joinToString(separator = ".")
        }
}

private val BridgeRequest.descriptor: BridgeFunctionDescriptor
    get() {
        require(callable !is SirInit) { "Use allocationDescriptor and initializationDescriptor instead" }
        return BridgeFunctionDescriptor(
            bridgeName,
            callable.bridgeParameters(),
            bridgeType(callable.returnType),
            fqName,
            if (callable.kind == SirCallableKind.INSTANCE_METHOD) {
                val selfType = when (callable) {
                    is SirFunction -> SirNominalType(callable.parent as SirClass)
                    is SirAccessor -> SirNominalType((callable.parent as SirVariable).parent as SirClass)
                    is SirInit -> error("Init node cannot be an instance method")
                }
                BridgeParameter("self", bridgeType(selfType))
            } else null,
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
            fqName,
            null,
        )
    }

private val BridgeRequest.initializationDescriptor: BridgeFunctionDescriptor
    get() {
        require(callable is SirInit) { "Use descriptor instead" }
        return BridgeFunctionDescriptor(
            bridgeName + "_initialize",
            listOf(obj) + callable.bridgeParameters(),
            bridgeType(callable.returnType),
            fqName,
            null,
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
    typeNamer: SirTypeNamer,
    buildCallSite: (name: String, args: List<String>) -> String,
) = buildList {
    add("@${exportAnnotationFqName.substringAfterLast('.')}(\"${cBridgeName}\")")
    add("public fun $kotlinBridgeName(${allParameters.joinToString { "${it.name}: ${it.bridge.kotlinType.repr}" }}): ${returnType.kotlinType.repr} {")
    val indent = "    "
    allParameters.forEach {
        add("${indent}val __${it.name} = ${it.bridge.inKotlinSources.swiftToKotlin(typeNamer, it.name)}")
    }
    val callSite = buildCallSite(kotlinName, parameters.map { "__${it.name}" })
    if (returnType.swiftType.isVoid) {
        add("${indent}$callSite")
    } else {
        val resultName = "_result"
        add("${indent}val $resultName = $callSite")
        add("${indent}return ${returnType.inKotlinSources.kotlinToSwift(typeNamer, resultName)}")
    }
    add("}")
}

private fun BridgeFunctionDescriptor.swiftCall(typeNamer: SirTypeNamer): String {
    val call = "$cBridgeName(${allParameters.joinToString { it.bridge.inSwiftSources.swiftToKotlin(typeNamer, it.name) }})"
    return returnType.inSwiftSources.kotlinToSwift(typeNamer, call)
}

private fun BridgeFunctionDescriptor.cDeclaration() =
    "${returnType.cType.repr} ${cBridgeName}(${allParameters.joinToString { "${it.bridge.cType.repr} ${it.name}" }});"

private inline fun BridgeFunctionDescriptor.createFunctionBridge(typeNamer: SirTypeNamer, kotlinCall: (name: String, args: List<String>) -> String) =
    FunctionBridge(
        KotlinFunctionBridge(createKotlinBridge(typeNamer, kotlinCall), listOf(exportAnnotationFqName)),
        CFunctionBridge(listOf(cDeclaration()), listOf(stdintHeader))
    )

private fun SirCallable.bridgeParameters() = allParameters.mapIndexed { index, value -> bridgeParameter(value, index) }

private fun bridgeType(type: SirType): Bridge {
    require(type is SirNominalType)
    return when (val subtype = type.type) {
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

        SirSwiftModule.uint -> Bridge.AsOpaqueObject(type, KotlinType.Object, CType.Object)

        is SirTypealias -> bridgeType(subtype.type)

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

/**
 * Generate value conversions between Swift and Kotlin.
 */
private interface ValueConversion {
    fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String
    fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String
}

private object IdentityValueConversion : ValueConversion {
    override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = valueExpression
    override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) = valueExpression
}

private sealed class Bridge(
    val swiftType: SirType,
    val kotlinType: KotlinType,
    val cType: CType,
) {
    class AsIs(swiftType: SirType, kotlinType: KotlinType, cType: CType) : Bridge(swiftType, kotlinType, cType) {
        override val inKotlinSources = IdentityValueConversion
        override val inSwiftSources = IdentityValueConversion
    }

    class AsObject(swiftType: SirType, kotlinType: KotlinType, cType: CType) : Bridge(swiftType, kotlinType, cType) {
        override val inKotlinSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression) as ${typeNamer.kotlinFqName(swiftType)}"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = "${valueExpression}.__externalRCRef()"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "${typeNamer.swiftFqName(swiftType)}(__externalRCRef: $valueExpression)"

        }
    }

    class AsOpaqueObject(swiftType: SirType, kotlinType: KotlinType, cType: CType) : Bridge(swiftType, kotlinType, cType) {
        override val inKotlinSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression)"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = IdentityValueConversion
    }

    /**
     * [ValueConversion] to be used when generating Kotlin sources.
     */
    abstract val inKotlinSources: ValueConversion

    /**
     * [ValueConversion] to be used when generating Swift sources.
     */
    abstract val inSwiftSources: ValueConversion
}
