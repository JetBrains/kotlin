/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.BridgeProvider

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.util.SirPlatformModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.isNever
import org.jetbrains.kotlin.sir.util.name

internal fun bridgeType(type: SirType): Bridge = when (type) {
    is SirNominalType -> bridgeNominalType(type)
    is SirExistentialType -> bridgeExistential(type)
    is SirFunctionalType -> Bridge.AsBlock(type)
    else -> error("Attempt to bridge unbridgeable type: $type.")
}

private fun bridgeExistential(type: SirExistentialType): Bridge = Bridge.AsExistential(
    swiftType = type,
    KotlinType.KotlinObject,
    CType.Object
)

private fun bridgeNominalType(type: SirNominalType): Bridge {
    fun bridgeAsNSCollectionElement(type: SirType): Bridge = when (val bridge = bridgeType(type)) {
        is Bridge.AsIs -> Bridge.AsNSNumber(bridge.swiftType)
        is Bridge.AsOptionalWrapper -> Bridge.AsObjCBridgedOptional(bridge.wrappedObject.swiftType)
        is Bridge.AsOptionalNothing -> Bridge.AsObjCBridgedOptional(bridge.swiftType)
        is Bridge.AsObject,
        is Bridge.AsExistential,
        is Bridge.AsOpaqueObject,
            -> Bridge.AsObjCBridged(bridge.swiftType, CType.id)
        else -> bridge
    }

    return when (val subtype = type.typeDeclaration) {
        SirSwiftModule.void -> Bridge.AsVoid

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

        SirSwiftModule.unsafeMutableRawPointer -> Bridge.AsOpaqueObject(type, KotlinType.KotlinObject, CType.Object)
        SirSwiftModule.never -> Bridge.AsOpaqueObject(type, KotlinType.KotlinObject, CType.Void)

        SirSwiftModule.string -> Bridge.AsObjCBridged(type, CType.NSString)

        SirSwiftModule.utf16CodeUnit -> Bridge.AsIs(type, KotlinType.Char, CType.UInt16)

        SirSwiftModule.optional -> when (val bridge = bridgeType(type.typeArguments.first())) {
            is Bridge.AsObject,
            is Bridge.AsObjCBridged,
            is Bridge.AsExistential,
            is Bridge.AsBlock,
                -> Bridge.AsOptionalWrapper(bridge)

            is Bridge.AsOpaqueObject -> {
                if (bridge.swiftType.isNever) {
                    Bridge.AsOptionalNothing
                } else {
                    error("Found Optional wrapping for OpaqueObject. That is impossible")
                }
            }

            is Bridge.AsIs,
                -> Bridge.AsOptionalWrapper(
                if (bridge.swiftType.isChar)
                    Bridge.OptionalChar(bridge.swiftType)
                else
                    Bridge.AsNSNumber(bridge.swiftType)
            )

            else -> error("Found Optional wrapping for $bridge. That is currently unsupported. See KT-66875")
        }

        SirSwiftModule.array -> Bridge.AsNSArray(type, bridgeAsNSCollectionElement(type.typeArguments.single()))
        SirSwiftModule.set -> Bridge.AsNSSet(type, bridgeAsNSCollectionElement(type.typeArguments.single()))
        SirSwiftModule.dictionary -> {
            val (key, value) = type.typeArguments
            Bridge.AsNSDictionary(
                type,
                bridgeAsNSCollectionElement(key),
                bridgeAsNSCollectionElement(value)
            )
        }

        is SirTypealias -> bridgeType(subtype.type)

        // TODO: Right now, we just assume everything nominal that we do not recognize is a class. We should make this decision looking at kotlin type?
        else -> if (type.typeDeclaration.parent is SirPlatformModule) {
            Bridge.AsNSObject(type)
        } else {
            Bridge.AsObject(type, KotlinType.KotlinObject, CType.Object)
        }
    }
}

internal fun bridgeParameter(parameter: SirParameter, index: Int): BridgeParameter {
    val bridgeParameterName = parameter.name?.let(::createBridgeParameterName) ?: "_$index"
    val bridge = bridgeType(parameter.type)
    return BridgeParameter(
        name = bridgeParameterName,
        bridge = bridge
    )
}

internal data class BridgeParameter(
    val name: String,
    val bridge: Bridge,
) {
    var isRenderable: Boolean = bridge !is Bridge.AsOptionalNothing && bridge !is Bridge.AsVoid
}

internal val SirType.isChar: Boolean
    get() = this is SirNominalType && typeDeclaration == SirSwiftModule.utf16CodeUnit

/**
 * Generate value conversions between Swift and Kotlin.
 */
internal interface ValueConversion {
    fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String
    fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String
}

internal interface NilRepresentable {
    fun renderNil(): String
}

internal object IdentityValueConversion : ValueConversion {
    override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = valueExpression
    override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) = valueExpression
}

internal interface NilableIdentityValueConversion : Bridge.InSwiftSourcesConversion {
    override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = valueExpression
    override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) = valueExpression
}

private fun String.mapSwift(temporalName: String = "it", transform: (String) -> String): String {
    val adapter = transform(temporalName).takeIf { it != temporalName }
    return this + (adapter?.let { ".map { $temporalName in $it }" } ?: "")
}

internal sealed class Bridge(
    open val swiftType: SirType,
    val kotlinType: KotlinType,
    open val cType: CType,
) {
    class AsIs(swiftType: SirType, kotlinType: KotlinType, cType: CType) : Bridge(swiftType, kotlinType, cType) {
        override val inKotlinSources = IdentityValueConversion
        override val inSwiftSources = object : NilableIdentityValueConversion {
            override fun renderNil(): String = "nil"
        }
    }

    object AsVoid : Bridge(SirNominalType(SirSwiftModule.void), KotlinType.Unit, CType.Void) {
        override val inKotlinSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String = "Unit"
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String = valueExpression

        }
        override val inSwiftSources = object : NilableIdentityValueConversion {
            override fun renderNil(): String = "nil"
        }
    }


    class AsObject(swiftType: SirType, kotlinType: KotlinType, cType: CType) : Bridge(swiftType, kotlinType, cType) {
        override val inKotlinSources = object : ValueConversion {
            // nulls are handled by AsOptionalWrapper, so safe to cast from nullable to non-nullable
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression) as ${
                    typeNamer.kotlinFqName(
                        swiftType,
                        SirTypeNamer.KotlinNameType.PARAMETRIZED
                    )
                }"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : InSwiftSourcesConversion {
            override fun renderNil(): String = "nil"

            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = "${valueExpression}.__externalRCRef()"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "${typeNamer.swiftFqName(swiftType)}.__createClassWrapper(externalRCRef: $valueExpression)"
        }
    }

    class AsExistential(swiftType: SirExistentialType, kotlinType: KotlinType, cType: CType) : Bridge(swiftType, kotlinType, cType) {
        override val inKotlinSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression) as ${
                    typeNamer.kotlinFqName(
                        swiftType,
                        SirTypeNamer.KotlinNameType.PARAMETRIZED
                    )
                }"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : InSwiftSourcesConversion {
            override fun renderNil(): String = "nil"

            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = "${valueExpression}.__externalRCRef()"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "${typeNamer.swiftFqName(SirNominalType(KotlinRuntimeModule.kotlinBase))}.__createProtocolWrapper(externalRCRef: $valueExpression) as! ${typeNamer.swiftFqName(swiftType)}"
        }
    }

    class AsOpaqueObject(swiftType: SirType, kotlinType: KotlinType, cType: CType) : Bridge(swiftType, kotlinType, cType) {
        override val inKotlinSources = object : ValueConversion {
            // nulls are handled by AsOptionalWrapper, so safe to cast from nullable to non-nullable
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression)!!"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : NilableIdentityValueConversion {
            override fun renderNil(): String = "nil"
        }
    }

    open class AsObjCBridged(
        swiftType: SirType,
        cType: CType,
    ) : Bridge(swiftType, KotlinType.ObjCObjectUnretained, cType) {
        override val inKotlinSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "interpretObjCPointer<${typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)}>($valueExpression)"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "$valueExpression.objcPtr()"
        }

        override val inSwiftSources: InSwiftSourcesConversion = object : NilableIdentityValueConversion {
            override fun renderNil(): String = "nil"
        }
    }

    /** To be used inside NS* collections. `null`s are wrapped as `NSNull`.  */
    open class AsObjCBridgedOptional(
        swiftType: SirType,
    ) : AsObjCBridged(swiftType, CType.id) {

        override val inSwiftSources = object : NilableIdentityValueConversion {
            override fun renderNil(): String = "NSNull()"

            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                return "$valueExpression as NSObject? ?? ${renderNil()}"
            }

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String = valueExpression
        }
    }

    open class AsNSNumber(
        swiftType: SirType,
    ) : AsObjCBridged(swiftType, CType.NSNumber) {
        override val inSwiftSources = object : NilableIdentityValueConversion {
            override fun renderNil(): String = super@AsNSNumber.inSwiftSources.renderNil()

            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "NSNumber(value: $valueExpression)"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                require(swiftType is SirNominalType)
                val fromNSNumberValue = when (swiftType.typeDeclaration) {
                    SirSwiftModule.bool -> "boolValue"
                    SirSwiftModule.int8 -> "int8Value"
                    SirSwiftModule.int16 -> "int16Value"
                    SirSwiftModule.int32 -> "int32Value"
                    SirSwiftModule.int64 -> "int64Value"
                    SirSwiftModule.uint8 -> "uint8Value"
                    SirSwiftModule.uint16 -> "uint16Value"
                    SirSwiftModule.uint32 -> "uint32Value"
                    SirSwiftModule.uint64 -> "uint64Value"
                    SirSwiftModule.double -> "doubleValue"
                    SirSwiftModule.float -> "floatValue"

                    SirSwiftModule.utf16CodeUnit -> "uint16Value"

                    else -> error("Attempt to get ${swiftType.typeDeclaration} from NSNumber")
                }

                return "$valueExpression.$fromNSNumberValue"
            }
        }
    }

    class OptionalChar(swiftType: SirType) : AsNSNumber(swiftType) {
        init {
            require(swiftType.isChar)
        }

        override val inKotlinSources = object : ValueConversion by super.inKotlinSources {
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                super@OptionalChar.inKotlinSources.kotlinToSwift(typeNamer, "${valueExpression}?.code")
        }
    }

    class AsNSObject(
        swiftType: SirNominalType,
    ) : AsObjCBridged(swiftType, CType.NSObject) {
        override val inSwiftSources: InSwiftSourcesConversion = object : NilableIdentityValueConversion {
            override fun renderNil(): String = "nil"
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "$valueExpression as! ${typeNamer.swiftFqName(swiftType)}"
        }
    }

    abstract class AsNSCollection(
        swiftType: SirNominalType,
        cType: CType,
    ) : AsObjCBridged(swiftType, cType) {
        abstract inner class InSwiftSources : InSwiftSourcesConversion {
            override fun renderNil(): String = super@AsNSCollection.inSwiftSources.renderNil()

            abstract override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                return "$valueExpression as! ${typeNamer.swiftFqName(swiftType)}"
            }
        }
    }

    class AsNSArray(swiftType: SirNominalType, elementBridge: Bridge) : AsNSCollection(swiftType, CType.NSArray(elementBridge.cType)) {
        override val inSwiftSources = object : InSwiftSources() {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                return valueExpression.mapSwift { elementBridge.inSwiftSources.swiftToKotlin(typeNamer, it) }
            }
        }
    }

    class AsNSSet(swiftType: SirNominalType, elementBridge: Bridge) : AsNSCollection(swiftType, CType.NSSet(elementBridge.cType)) {
        override val inSwiftSources = object : InSwiftSources() {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val transformedElements = valueExpression.mapSwift { elementBridge.inSwiftSources.swiftToKotlin(typeNamer, it) }
                return if (transformedElements == valueExpression) valueExpression else "Set($transformedElements)"
            }
        }
    }

    class AsNSDictionary(swiftType: SirNominalType, val keyBridge: Bridge, val valueBridge: Bridge) :
        AsNSCollection(swiftType, CType.NSDictionary(keyBridge.cType, valueBridge.cType)) {

        override val inSwiftSources = object : InSwiftSources() {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val keyAdapter = keyBridge.inSwiftSources.swiftToKotlin(typeNamer, "key")
                val valueAdapter = valueBridge.inSwiftSources.swiftToKotlin(typeNamer, "value")
                return if (keyAdapter == "key" && valueAdapter == "value") {
                    valueExpression
                } else {
                    "Dictionary(uniqueKeysWithValues: $valueExpression.map { key, value in (" +
                            "${keyBridge.inSwiftSources.swiftToKotlin(typeNamer, "key")}, " +
                            "${valueBridge.inSwiftSources.swiftToKotlin(typeNamer, "value")} " +
                            ")})"
                }
            }
        }
    }

    data object AsOptionalNothing : Bridge(
        SirNominalType(SirSwiftModule.optional, listOf(SirNominalType(SirSwiftModule.never))),
        KotlinType.Unit,
        CType.Void
    ) {
        override val inKotlinSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = "null"
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) = "Unit"
        }

        override val inSwiftSources = object : InSwiftSourcesConversion {
            override fun renderNil(): String = error("unrepresentable")
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = renderNil()
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "{ ${valueExpression}; return nil; }()"
        }
    }

    class AsOptionalWrapper(
        val wrappedObject: Bridge,
    ) : Bridge(
        wrappedObject.swiftType.optional(),
        wrappedObject.kotlinType,
        wrappedObject.cType.nullable
    ) {

        override val inKotlinSources: ValueConversion
            get() = object : ValueConversion {
                override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                    return "if ($valueExpression == kotlin.native.internal.NativePtr.NULL) null else ${
                        wrappedObject.inKotlinSources.swiftToKotlin(typeNamer, valueExpression)
                    }"
                }

                override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                    return "if ($valueExpression == null) kotlin.native.internal.NativePtr.NULL else ${
                        wrappedObject.inKotlinSources.kotlinToSwift(typeNamer, valueExpression)
                    }"
                }
            }

        override val inSwiftSources: InSwiftSourcesConversion = object : InSwiftSourcesConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                require(wrappedObject is AsObjCBridged || wrappedObject is AsObject || wrappedObject is AsExistential || wrappedObject is AsBlock)
                return valueExpression.mapSwift { wrappedObject.inSwiftSources.swiftToKotlin(typeNamer, it) } +
                        " ?? ${wrappedObject.inSwiftSources.renderNil()}"
            }

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                return when (wrappedObject) {
                    is AsObjCBridged ->
                        valueExpression.mapSwift { wrappedObject.inSwiftSources.kotlinToSwift(typeNamer, it) }
                    is AsObject, is AsExistential, is AsBlock -> "{ switch $valueExpression { case ${wrappedObject.inSwiftSources.renderNil()}: .none; case let res: ${
                        wrappedObject.inSwiftSources.kotlinToSwift(typeNamer, "res")
                    }; } }()"
                    is AsIs,
                    is AsVoid,
                    is AsOpaqueObject,
                    is AsOutError,
                        -> TODO("not yet supported")

                    is AsOptionalWrapper, AsOptionalNothing -> error("there is not optional wrappers for optional")
                }
            }

            override fun renderNil(): String = error("we do not support wrapping optionals into optionals, as it is impossible in kotlin")
        }
    }

    class AsBlock(
        override val swiftType: SirFunctionalType,
        private val parameters: List<Bridge> = swiftType.parameterTypes.map(::bridgeType),
        private val returnType: Bridge = bridgeType(swiftType.returnType),
    ) : Bridge(
        swiftType = swiftType,
        kotlinType = KotlinType.KotlinObject,
        cType = CType.BlockPointer(
            parameters = parameters.map { it.cType },
            returnType = returnType.cType,
        )
    ) {
        override val cType: CType.BlockPointer
            get() = super.cType as? CType.BlockPointer
                ?: error("attempt to generate kotlin sources for handling closure fot a type that is not closure")

        private val kotlinFunctionTypeRendered = "(${parameters.joinToString { it.kotlinType.repr }})->${returnType.kotlinType.repr}"

        override val inKotlinSources: ValueConversion
            get() = object : ValueConversion {
                override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                    val argsInClosure = parameters
                        .mapIndexed { idx, el -> "arg${idx}" to el }.takeIf { it.isNotEmpty() }
                    val defineArgs = argsInClosure
                        ?.let {
                            " ${
                                it.joinToString {
                                    "${it.first}: ${
                                        typeNamer.kotlinFqName(
                                            it.second.swiftType,
                                            SirTypeNamer.KotlinNameType.PARAMETRIZED
                                        )
                                    }"
                                }
                            } ->"
                        }
                    val callArgs = argsInClosure
                        ?.let { it.joinToString { it.second.inKotlinSources.kotlinToSwift(typeNamer, it.first) } } ?: ""
                    return """run {    
                    |    val kotlinFun = convertBlockPtrToKotlinFunction<$kotlinFunctionTypeRendered>($valueExpression);
                    |    {${defineArgs ?: ""}
                    |        val _result = kotlinFun($callArgs)
                    |        ${returnType.inKotlinSources.swiftToKotlin(typeNamer, "_result")} 
                    |    }
                    |}""".replaceIndentByMargin("    ")
                }

                override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                    return """run {
                    |    val newClosure = { kotlin.native.internal.ref.createRetainedExternalRCRef(_result()).toLong() }
                    |    newClosure.objcPtr()
                    |}""".replaceIndentByMargin("    ")
                }
            }

        override val inSwiftSources: InSwiftSourcesConversion = object : InSwiftSourcesConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val argsInClosure = parameters
                    .mapIndexed { idx, el -> "arg${idx}" to el }.takeIf { it.isNotEmpty() }
                val defineArgs = argsInClosure
                    ?.let { " ${it.joinToString { it.first }} in" } ?: ""
                val callArgs = argsInClosure
                    ?.let {
                        it.joinToString { param ->
                            param.second.inSwiftSources.kotlinToSwift(typeNamer, param.first)
                        }
                    } ?: ""
                return """{
                |    let originalBlock = $valueExpression
                |    return {$defineArgs ${"return ${returnType.inSwiftSources.swiftToKotlin(typeNamer, "originalBlock($callArgs)")}"} }
                |}()""".trimMargin()
            }

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                return """{
                |    let nativeBlock = $valueExpression
                |    return { nativeBlock() }
                |}()""".trimMargin()
            }

            override fun renderNil(): String = "nil"
        }
    }

    object AsOutError : Bridge(
        swiftType = SirType.never,
        kotlinType = KotlinType.PointerToKotlinObject,
        cType = CType.OutObject.nonnulll,
    ) {
        override val inKotlinSources: ValueConversion
            get() = IdentityValueConversion

        override val inSwiftSources: InSwiftSourcesConversion
            get() = object : InSwiftSourcesConversion {
                override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                    return "&$valueExpression"
                }

                override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                    return "${typeNamer.swiftFqName(SirNominalType(KotlinRuntimeModule.kotlinBase))}.__createClassWrapper(externalRCRef: $valueExpression)"
                }

                override fun renderNil(): String {
                    return "nil"
                }
            }
    }

    /**
     * [ValueConversion] to be used when generating Kotlin sources.
     */
    abstract val inKotlinSources: ValueConversion

    /**
     * [ValueConversion] to be used when generating Swift sources.
     */
    abstract val inSwiftSources: InSwiftSourcesConversion

    interface InSwiftSourcesConversion : ValueConversion, NilRepresentable
}