/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.BridgeProvider

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.*
import org.jetbrains.kotlin.sir.providers.sirModule
import org.jetbrains.kotlin.sir.providers.toBridge
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.util.SirPlatformModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.isNever
import org.jetbrains.kotlin.sir.util.isValueType
import org.jetbrains.kotlin.sir.util.name
import org.jetbrains.kotlin.sir.util.swiftName
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

context(session: SirSession)
internal fun bridgeType(type: SirType): Bridge =
    bridgeType(type, SirTypeVariance.INVARIANT) as Bridge

context(session: SirSession)
internal fun bridgeParameterType(type: SirType): SwiftToKotlinBridge =
    bridgeType(type, SirTypeVariance.CONTRAVARIANT) as SwiftToKotlinBridge

context(session: SirSession)
internal fun bridgeReturnType(type: SirType): KotlinToSwiftBridge =
    bridgeType(type, SirTypeVariance.COVARIANT) as KotlinToSwiftBridge

context(session: SirSession)
private fun bridgeTypeForVariadicParameter(type: SirType): AnyBridge =
    AsNSArrayForVariadic(SirArrayType(type), bridgeAsNSCollectionElement(type))

context(session: SirSession)
internal fun bridgeType(type: SirType, position: SirTypeVariance): AnyBridge =
    when (type) {
        is SirNominalType -> bridgeNominalType(type, position)
        is SirExistentialType -> bridgeExistential(type, position)
        is SirFunctionalType -> when (position) {
            SirTypeVariance.COVARIANT -> AsCovariantBlock(type)
            SirTypeVariance.CONTRAVARIANT -> AsContravariantBlock(type)
            SirTypeVariance.INVARIANT -> error("Attempt to bridge functional type as invariant: $type")
        }
        else -> error("Attempt to bridge unbridgeable type: $type.")
    }

private fun bridgeExistential(type: SirExistentialType, position: SirTypeVariance): AnyBridge {
    if (type.protocols.singleOrNull() == KotlinRuntimeSupportModule.kotlinBridgeable) {
        return AsAnyBridgeable
    }
    return AsExistential(
        swiftType = type,
        KotlinType.KotlinObject,
        CType.Object
    )
}

context(session: SirSession)
internal fun bridgeAsNSCollectionElement(type: SirType): WithSingleType = when (val bridge = bridgeType(type)) {
    is AsIs -> AsNSNumber(bridge.swiftType)
    is AsOptionalWrapper -> AsObjCBridgedOptional(bridge.wrappedObject.swiftType)
    is AsOptionalNothing -> AsObjCBridgedOptional(bridge.swiftType)
    is AsObject,
    is AsExistential,
    is AsAnyBridgeable,
    is AsOpaqueObject,
    is SirCustomTypeTranslatorImpl.RangeBridge
        -> AsObjCBridged(bridge.swiftType, CType.id)
    is AsObjCBridged,
    AsOutError,
    AsVoid,
        -> bridge as WithSingleType
}

context(session: SirSession)
private fun bridgeNominalType(type: SirNominalType, position: SirTypeVariance): AnyBridge {
    val customTypeBridgeWrapper = type.toBridge()
    if (customTypeBridgeWrapper != null) return customTypeBridgeWrapper.bridge
    return when (val subtype = type.typeDeclaration) {
        SirSwiftModule.unsafeMutableRawPointer -> AsOpaqueObject(type, KotlinType.KotlinObject, CType.Object)
        SirSwiftModule.never -> AsOpaqueObject(type, KotlinType.KotlinObject, CType.Void)

        SirSwiftModule.optional -> when (val bridge = bridgeType(type.typeArguments.first(), position)) {
            is AsObject,
            is AsObjCBridged,
            is AsExistential,
            is AsAnyBridgeable,
            is AsContravariantBlock,
            is AsCovariantBlock,
            is SirCustomTypeTranslatorImpl.RangeBridge
                -> AsOptionalWrapper(bridge)

            is AsOpaqueObject -> {
                if (bridge.swiftType.isNever) {
                    AsOptionalNothing
                } else {
                    error("Found Optional wrapping for OpaqueObject. That is impossible")
                }
            }

            is AsIs,
                -> AsOptionalWrapper(
                if (bridge.swiftType.isChar)
                    OptionalChar(bridge.swiftType)
                else
                    AsNSNumber(bridge.swiftType)
            )

            else -> error("Found Optional wrapping for $bridge. That is currently unsupported. See KT-66875")
        }

        is SirTypealias -> bridgeType(subtype.type, position)

        // TODO: Right now, we just assume everything nominal that we do not recognize is a class. We should make this decision looking at kotlin type?
        else -> if (type.typeDeclaration.parent is SirPlatformModule) {
            AsNSObject(type)
        } else {
            AsObject(type, KotlinType.KotlinObject, CType.Object)
        }
    }
}

context(session: SirSession)
internal fun bridgeParameter(parameter: SirParameter, index: Int): BridgedParameter {
    val bridgeParameterName = parameter.name?.let(::createBridgeParameterName) ?: "_$index"
    val bridge = if (parameter.isVariadic) {
        bridgeTypeForVariadicParameter(parameter.type) as SwiftToKotlinBridge
    } else {
        bridgeParameterType(parameter.type)
    }
    return BridgedParameter.In(
        name = bridgeParameterName,
        bridge = bridge,
        isExplicit = parameter.origin != null,
    )
}

internal sealed interface BridgedParameter {
    val name: String
    val bridge: SwiftToKotlinBridge
    val isExplicit: Boolean

    data class In(
        override val name: String,
        override val bridge: SwiftToKotlinBridge,
        override val isExplicit: Boolean = false,
    ) : BridgedParameter

    data class InOut(
        override val name: String,
        override val bridge: Bridge,
        override val isExplicit: Boolean = false,
    ) : BridgedParameter

    val isRenderable: Boolean get() = bridge !is AsOptionalNothing && bridge !is AsVoid
}

internal val SirType.isChar: Boolean
    get() = this is SirNominalType && typeDeclaration == SirSwiftModule.utf16CodeUnit

/**
 * Generate value conversions from Swift to Kotlin.
 */
internal interface SwiftToKotlinValueConversion {
    fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String = valueExpression
}

/**
 * Generate value conversions from Kotlin to Swift.
 */
internal interface KotlinToSwiftValueConversion {
    fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String = valueExpression
}

/**
 * Generate value conversions between Swift and Kotlin.
 */
internal interface ValueConversion : SwiftToKotlinValueConversion, KotlinToSwiftValueConversion

internal object IdentityValueConversion : ValueConversion {
    override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = valueExpression
    override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) = valueExpression
}

private fun String.mapSwift(temporalName: String = "it", transform: (String) -> String): String {
    val adapter = transform(temporalName).takeIf { it != temporalName }
    return this + (adapter?.let { ".map { $temporalName in $it }" } ?: "")
}

internal sealed interface AnyBridge {
    val swiftType: SirType
    val typeList: List<TypePair>

    fun nativePointerToMultipleObjCBridge(index: Int): SirFunctionBridge

    context(sir: SirSession)
    fun helperBridges(typeNamer: SirTypeNamer): List<SirBridge> = emptyList()

    data class TypePair(val kotlinType: KotlinType, val cType: CType)
}

internal sealed interface KotlinToSwiftBridge : AnyBridge {
    val inKotlinSources: KotlinToSwiftValueConversion
    val inSwiftSources: KotlinToSwiftValueConversion
}

internal sealed interface SwiftToKotlinBridge : AnyBridge {
    val inKotlinSources: SwiftToKotlinValueConversion
    val inSwiftSources: SwiftToKotlinValueConversion
}

/**
 * A bridge that performs type conversions between three languages: Kotlin, Swift, and Objective-C.
 *
 * More specifically, a bridge is a code generation strategy that maps certain types and
 * handles value conversions for those types across three stages of a Swift interop call:
 * a Swift function, a bridge-level Kotlin function operating under the Objective-C calling convention
 * (referred to below as the Kotlin wrapper function), and the original Kotlin function.
 *
 * Note: Each parameter requires its own bridge (for the conversion Swift → Kotlin/ObjC → Kotlin),
 * and the return value requires a separate bridge (for the reverse conversion Kotlin → Kotlin/ObjC → Swift).
 *
 * The general sequence is as follows:
 * 1. The Swift function receives arguments from the caller (as Swift types).
 * 2. It converts its parameters for the Kotlin wrapper (Objective-C) using [inSwiftSources].swiftToKotlin.
 * 3. The Kotlin wrapper function gets invoked, accepting arguments as Objective-C values.
 * 4. It converts its parameters into Kotlin format using [inKotlinSources].swiftToKotlin.
 * 5. The original Kotlin function is called, producing a return value in Kotlin format.
 * 6. The Kotlin wrapper function converts this return value back into its own Objective-C format using [inKotlinSources].kotlinToSwift.
 * 7. The result is returned to the Swift function.
 * 8. Finally, the Swift function converts the result from Objective-C to Swift using [inSwiftSources].kotlinToSwift and returns it to the caller.
 */
internal sealed class Bridge(
    override val swiftType: SirType,
    override val typeList: List<AnyBridge.TypePair>,
) : SwiftToKotlinBridge, KotlinToSwiftBridge {
    abstract override val inKotlinSources: ValueConversion
    abstract override val inSwiftSources: ValueConversion

    sealed interface AnyBridgeWithSingleType : AnyBridge {
        val kotlinType: KotlinType get() = typeList.single().kotlinType

        val cType: CType get() = typeList.single().cType

        override fun nativePointerToMultipleObjCBridge(index: Int): SirFunctionBridge {
            throw UnsupportedOperationException("Should never be called if a bridge has single ObjC type")
        }
    }

    sealed class WithSingleType(
        swiftType: SirType,
        kotlinType: KotlinType,
        cType: CType,
    ) : Bridge(swiftType, listOf(AnyBridge.TypePair(kotlinType, cType))), AnyBridgeWithSingleType

    sealed class SwiftToKotlinBridgeWithSingleType(
        override val swiftType: SirType,
        kotlinType: KotlinType,
        cType: CType,
    ) : SwiftToKotlinBridge, AnyBridgeWithSingleType {
        override val typeList: List<AnyBridge.TypePair> = listOf(AnyBridge.TypePair(kotlinType, cType))
    }

    sealed class KotlinToSwiftBridgeWithSingleType(
        override val swiftType: SirType,
        kotlinType: KotlinType,
        cType: CType,
    ) : KotlinToSwiftBridge, AnyBridgeWithSingleType {
        override val typeList: List<AnyBridge.TypePair> = listOf(AnyBridge.TypePair(kotlinType, cType))
    }

    /**
     * A bridge that performs an as-is (trivial) conversion.
     *
     * This type of bridge is primarily used for primitive types (integers, floating-point numbers, booleans, and unicode units).
     * No conversions are performed here: values are represented identically
     * across all three languages (Kotlin, Swift, and Objective-C).
     *
     */
    class AsIs(swiftType: SirType, kotlinType: KotlinType, cType: CType) : WithSingleType(swiftType, kotlinType, cType) {
        constructor(swiftDeclaration: SirScopeDefiningDeclaration, kotlinType: KotlinType, cType: CType) : this(
            SirNominalType(swiftDeclaration), kotlinType, cType
        )

        override val inKotlinSources = IdentityValueConversion
        override val inSwiftSources = IdentityValueConversion
    }

    object AsVoid : WithSingleType(SirNominalType(SirSwiftModule.void), KotlinType.Unit, CType.Void) {
        override val inKotlinSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String = "Unit"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String = valueExpression
        }

        override val inSwiftSources = IdentityValueConversion
    }

    object AsOutVoid : SwiftToKotlinBridgeWithSingleType(
        SirNominalType(SirSwiftModule.void),
        KotlinType.Unit,
        CType.Int32
    ) {
        override val inKotlinSources = object : SwiftToKotlinValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String = valueExpression
        }

        override val inSwiftSources = object : SwiftToKotlinValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String = "{ $valueExpression; return 0 }()"
        }
    }

    /**
     * A bridge that converts a Swift class-based type to the corresponding Kotlin class-based type using native pointers.
     *
     * This kind of bridge is currently used for all class-based types in Kotlin (except for a few specialized cases).
     * In Swift, such a type is also represented as a class.
     * In Kotlin wrapper functions (Objective-C), it is represented simply as a pointer (`NativePtr`).
     *
     * The sequence is as follows:
     * 1. The Swift function uses `_KotlinBridgeable.__externalRCRef` to obtain a GC-aware pointer to a Kotlin object, represented in Swift as `UnsafeMutableRawPointer`.
     * 2. The Kotlin wrapper function uses `dereferenceExternalRCRef(ptr) as KotlinClassName` to obtain a Kotlin object of the corresponding type from `NativePtr`.
     * 3. After receiving the Kotlin result, the wrapper function calls `createRetainedExternalRCRef(res)` to obtain the pointer back.
     * 4. Finally, the Swift function calls `__createClassWrapper(ptr)` to reconstruct the Swift value.
     *
     * Exception: Some Kotlin class-like types are represented by Swift value types (for example, enums) and consequently use some different bridge strategy instead.
     */
    class AsObject(swiftType: SirNominalType, kotlinType: KotlinType, cType: CType) : WithSingleType(swiftType, kotlinType, cType) {
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

        override val inSwiftSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = "${valueExpression}.__externalRCRef()"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val swiftFqName = typeNamer.swiftFqName(swiftType)
                return if (swiftType.isValueType) {
                    "$swiftFqName(__externalRCRefUnsafe: $valueExpression, options: .asBestFittingWrapper)"
                } else {
                    "$swiftFqName.__createClassWrapper(externalRCRef: $valueExpression)"
                }
            }
        }
    }

    /**
     * A bridge that converts a `_KotlinBridgeable` to `Any` using native pointers.
     *
     * This kind of bridge is currently used exclusively for the `Any` type in Kotlin.
     * Conceptually, it is a specialized version of [AsExistential], but with one key distinction:
     * `Any` is not an interface.
     *
     * In Swift, this is represented as `any _KotlinBridgeable` type.
     * In Kotlin wrapper functions (Objective-C), it is represented as a GC-aware pointer (passed as a `NativePtr`).
     *
     * The sequence is as follows:
     * 1. The Swift function uses `_KotlinBridgeable.__externalRCRef` to obtain a GC-aware pointer to a Kotlin object, represented in Swift as `UnsafeMutableRawPointer`.
     * 2. The Kotlin wrapper function uses `dereferenceExternalRCRef(ptr)` to obtain the Kotlin object as `Any`.
     * 3. After receiving the Kotlin result, the wrapper function calls `createRetainedExternalRCRef(res)` to obtain the pointer back.
     * 4. Finally, the Swift function calls `__createProtocolWrapper(ptr) as! _KotlinBridgeable` to reconstruct the Swift value.
     */
    object AsAnyBridgeable : WithSingleType(KotlinRuntimeSupportModule.kotlinBridgeableType, KotlinType.KotlinObject, CType.Object) {
        override val inKotlinSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression) as kotlin.Any"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources: ValueConversion = object : ValueConversion {
            override fun swiftToKotlin(
                typeNamer: SirTypeNamer,
                valueExpression: String,
            ): String {
                return "$valueExpression.__externalRCRef()"
            }

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "${typeNamer.swiftFqName(SirNominalType(KotlinRuntimeModule.kotlinBase))}.__createProtocolWrapper(externalRCRef: $valueExpression) as! ${typeNamer.swiftFqName(swiftType)}"
        }
    }

    /**
     * A bridge that converts a Swift existential type to the corresponding Kotlin interface type using native pointers.
     *
     * This type of bridge is currently used for all interface-based types in Kotlin (except for a few specialized cases).
     * In Swift, such a type is represented as a protocol-based existential type (`any P`).
     * In Kotlin wrapper functions (Objective-C), it is represented as a GC-aware pointer (passed as a `NativePtr`)..
     *
     * The sequence is as follows:
     * 1. The Swift function uses `_KotlinBridgeable.__externalRCRef` to obtain a GC-aware pointer to a Kotlin object, represented in Swift as `UnsafeMutableRawPointer`.
     * 2. The Kotlin wrapper function uses `dereferenceExternalRCRef(ptr) as KotlinInterfaceName` to obtain a Kotlin object of the corresponding type.
     * 3. After receiving the Kotlin result, the wrapper function calls `createRetainedExternalRCRef(res)` to obtain the pointer back.
     * 4. Finally, the Swift function calls `__createProtocolWrapper(ptr) as! SwiftProtocolName` to reconstruct the Swift value.
     */
    class AsExistential(swiftType: SirExistentialType, kotlinType: KotlinType, cType: CType) : WithSingleType(swiftType, kotlinType, cType) {
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

        override val inSwiftSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = "${valueExpression}.__externalRCRef()"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "${typeNamer.swiftFqName(SirNominalType(KotlinRuntimeModule.kotlinBase))}.__createProtocolWrapper(externalRCRef: $valueExpression) as! ${typeNamer.swiftFqName(swiftType)}"
        }
    }

    class AsOpaqueObject(swiftType: SirType, kotlinType: KotlinType, cType: CType) : WithSingleType(swiftType, kotlinType, cType) {
        override val inKotlinSources = object : ValueConversion {
            // nulls are handled by AsOptionalWrapper, so safe to cast from nullable to non-nullable
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression)!!"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = IdentityValueConversion
    }

    open class AsObjCBridged(
        swiftType: SirType,
        cType: CType,
    ) : WithSingleType(swiftType, KotlinType.ObjCObjectUnretained, cType) {
        override val inKotlinSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "interpretObjCPointer<${typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)}>($valueExpression)"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "$valueExpression.objcPtr()"
        }

        override val inSwiftSources: ValueConversion = IdentityValueConversion
    }

    /** To be used inside NS* collections. `null`s are wrapped as `NSNull`.  */
    open class AsObjCBridgedOptional(
        swiftType: SirType,
    ) : AsObjCBridged(swiftType, CType.id) {

        override val inSwiftSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                return "$valueExpression as! NSObject? ?? NSNull()"
            }

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String = valueExpression
        }
    }

    open class AsNSNumber(
        swiftType: SirType,
    ) : AsObjCBridged(swiftType, CType.NSNumber) {
        override val inKotlinSources = if (swiftType.isChar) {
            object : ValueConversion {
                // Surprisingly enough, `NSNumber(value: 0 as Swift.Unicode.UTF16.CodeUnit)` produces "i" encoding, as CFNumber internally
                // promotes `UInt16` (which `UTF16.CodeUnit` is) to the closest fitting signed integer storage (C int).
                // -[NSNumber toKotlin] then produces Int box when crossing bridge,
                // which doesn't pass strict cast checks near interpretObjCPointer
                override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                    "interpretObjCPointer<Int>($valueExpression).toChar()"

                override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                    "$valueExpression.objcPtr()"
            }
        } else {
            super.inKotlinSources
        }


        override val inSwiftSources = object : ValueConversion {
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
        override val inSwiftSources: ValueConversion = object : ValueConversion {
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "$valueExpression as! ${typeNamer.swiftFqName(swiftType)}"
        }
    }

    abstract class AsNSCollection(
        swiftType: SirNominalType,
        cType: CType,
    ) : AsObjCBridged(swiftType, cType) {
        abstract inner class InSwiftSources : ValueConversion {
            abstract override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                return "$valueExpression as! ${typeNamer.swiftFqName(swiftType)}"
            }
        }
    }

    open class AsNSArray(swiftType: SirNominalType, elementBridge: WithSingleType) : AsNSCollection(swiftType, CType.NSArray(elementBridge.cType)) {
        override val inSwiftSources = object : InSwiftSources() {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                return valueExpression.mapSwift { elementBridge.inSwiftSources.swiftToKotlin(typeNamer, it) }
            }
        }
    }

    class AsNSArrayForVariadic(swiftType: SirNominalType, elementBridge: WithSingleType) : AsNSArray(swiftType, elementBridge) {
        override val inKotlinSources: ValueConversion = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val arrayKind = typeNamer.kotlinPrimitiveFqNameIfAny(swiftType.typeArguments.single()) ?: "Typed"
                return "interpretObjCPointer<${
                    typeNamer.kotlinFqName(
                        swiftType,
                        SirTypeNamer.KotlinNameType.PARAMETRIZED
                    )
                }>($valueExpression).to${arrayKind}Array()"
            }

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "$valueExpression.objcPtr()"
        }
    }

    class AsNSSet(swiftType: SirNominalType, elementBridge: WithSingleType) : AsNSCollection(swiftType, CType.NSSet(elementBridge.cType)) {
        override val inSwiftSources = object : InSwiftSources() {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val transformedElements = valueExpression.mapSwift { elementBridge.inSwiftSources.swiftToKotlin(typeNamer, it) }
                return if (transformedElements == valueExpression) valueExpression else "Set($transformedElements)"
            }
        }
    }

    class AsNSDictionary(swiftType: SirNominalType, val keyBridge: WithSingleType, val valueBridge: WithSingleType) :
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

    data object AsOptionalNothing : WithSingleType(
        SirNominalType(SirSwiftModule.optional, listOf(SirNominalType(SirSwiftModule.never))),
        KotlinType.Unit,
        CType.Void
    ) {
        override val inKotlinSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = "null"
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) = "Unit"
        }

        override val inSwiftSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = error("unrepresentable")
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "{ ${valueExpression}; return nil; }()"
        }
    }

    class AsOptionalWrapper(
        val wrappedObject: AnyBridge,
    ) : Bridge(
        wrappedObject.swiftType.optional(),
        wrappedObject.typeList.map { AnyBridge.TypePair(it.kotlinType, it.cType.nullable) },
    ) {

        override val inKotlinSources: ValueConversion
            get() = object : ValueConversion {
                override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                    require(wrappedObject is SwiftToKotlinBridge)
                    return "if ($valueExpression == kotlin.native.internal.NativePtr.NULL) null else ${
                        wrappedObject.inKotlinSources.swiftToKotlin(typeNamer, valueExpression)
                    }"
                }

                override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                    require(wrappedObject is KotlinToSwiftBridge)
                    return "if ($valueExpression == null) kotlin.native.internal.NativePtr.NULL else ${
                        wrappedObject.inKotlinSources.kotlinToSwift(typeNamer, valueExpression)
                    }"
                }
            }

        override val inSwiftSources: ValueConversion = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                require(
                    wrappedObject is AsObjCBridged || wrappedObject is AsObject ||
                            wrappedObject is AsExistential || wrappedObject is AsAnyBridgeable || wrappedObject is AsContravariantBlock ||
                            wrappedObject is SirCustomTypeTranslatorImpl.RangeBridge
                )
                return valueExpression.mapSwift { wrappedObject.inSwiftSources.swiftToKotlin(typeNamer, it) } +
                        " ?? ${wrappedObject.renderNil()}"
            }

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                return when (wrappedObject) {
                    is AsObjCBridged, is AsCovariantBlock ->
                        valueExpression.mapSwift { wrappedObject.inSwiftSources.kotlinToSwift(typeNamer, it) }
                    is AsObject, is AsExistential, is AsAnyBridgeable, is SirCustomTypeTranslatorImpl.RangeBridge ->
                        "{ switch $valueExpression { case ${wrappedObject.renderNil()}: .none; case let res: ${
                            wrappedObject.inSwiftSources.kotlinToSwift(typeNamer, "res")
                        }; } }()"
                    is AsContravariantBlock,
                    is AsIs,
                    is AsVoid,
                    is AsOutVoid,
                    is AsOpaqueObject,
                    is AsOutError,
                        -> TODO("not yet supported")

                    is AsOptionalWrapper, AsOptionalNothing -> error("there is not optional wrappers for optional")
                }
            }
        }

        override fun nativePointerToMultipleObjCBridge(index: Int): SirFunctionBridge {
            // TODO: support Optional on Range(s) and other tuple-based types properly
            return wrappedObject.nativePointerToMultipleObjCBridge(index)
        }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer): List<SirBridge> {
            return wrappedObject.helperBridges(typeNamer)
        }
    }

    class AsContravariantBlock private constructor(
        override val swiftType: SirFunctionalType,
        private val parameters: List<KotlinToSwiftBridge>,
        private val returnType: SwiftToKotlinBridge,
    ) : SwiftToKotlinBridgeWithSingleType(
        swiftType = swiftType,
        kotlinType = KotlinType.KotlinObject,
        cType = CType.BlockPointer(
            // TODO: think about it, as types like ranges seems possible here making first() call illegal (?)
            parameters = parameters.map { it.typeList.first().cType },
            returnType = returnType.typeList.first().cType,
        )
    ) {
        private val kotlinFunctionTypeRendered =
            "(${parameters.joinToString { it.typeList.single().kotlinType.repr }})->${returnType.typeList.single().kotlinType.repr}"

        companion object {
            context(session: SirSession)
            operator fun invoke(
                swiftType: SirFunctionalType,
            ): AsContravariantBlock = AsContravariantBlock(
                swiftType,
                parameters = swiftType.parameterTypes.map { bridgeReturnType(it) },
                returnType = bridgeParameterType(swiftType.returnType),
            )

            operator fun invoke(
                parameters: List<KotlinToSwiftBridge>,
                returnType: SwiftToKotlinBridge,
            ): AsContravariantBlock = AsContravariantBlock(
                SirFunctionalType(
                    parameterTypes = parameters.map { it.swiftType.escaping },
                    returnType = returnType.swiftType,
                ),
                parameters,
                returnType,
            )
        }

        override val inKotlinSources: SwiftToKotlinValueConversion
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
            }

        override val inSwiftSources = object : SwiftToKotlinValueConversion {
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
        }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer): List<SirBridge> {
            return parameters.flatMap { it.helperBridges(typeNamer) } + returnType.helperBridges(typeNamer)
        }
    }

    class AsCovariantBlock private constructor(
        override val swiftType: SirFunctionalType,
        private val bridgeProxy: BridgeFunctionProxy,
    ) : KotlinToSwiftBridgeWithSingleType(
        swiftType = swiftType,
        kotlinType = KotlinType.KotlinObject,
        cType = CType.Object
    ) {
        companion object {
            context(session: SirSession)
            operator fun invoke(
                swiftType: SirFunctionalType,
            ): AsCovariantBlock = AsCovariantBlock(
                swiftType,
                bridgeProxy = session.generateFunctionBridge(
                    baseBridgeName = session.moduleToTranslate.sirModule().name + "_internal_functional_type_caller_" + swiftType.returnType.swiftName,
                    explicitParameters = listOf(
                        SirParameter(
                            argumentName = "pointerToBlock",
                            type = SirSwiftModule.unsafeMutableRawPointer.nominalType()
                        )
                    ) + swiftType.parameterTypes.map { SirParameter(type = it) },
                    returnType = swiftType.returnType,
                    kotlinFqName = FqName(""),
                    selfParameter = null,
                    contextParameters = emptyList(),
                    extensionReceiverParameter = null,
                    errorParameter = null,
                    isAsync = swiftType.isAsync
                )!!
            )

            context(session: SirSession)
            operator fun invoke(
                parameters: List<SwiftToKotlinBridge>,
                returnType: KotlinToSwiftBridge
            ): AsCovariantBlock = AsCovariantBlock(
                SirFunctionalType(
                    parameterTypes = parameters.map { it.swiftType.escaping },
                    returnType = returnType.swiftType,
                ),
            )
        }

        override val inKotlinSources: KotlinToSwiftValueConversion
            get() = object : ValueConversion {
                override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                    "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
            }

        override val inSwiftSources = object : KotlinToSwiftValueConversion {
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val allArgs = bridgeProxy.argumentsForInvocation().applyIf(swiftType.isAsync) { dropLast(3) }
                val defineArgs = allArgs
                    .drop(1).takeIf { it.isNotEmpty() }
                    ?.let { " ${it.joinToString()} in" } ?: ""
                return """{
                |    let ${allArgs.first()} = $valueExpression
                |    return {$defineArgs ${bridgeProxy.createSwiftInvocation({ "return $it" }).joinToString(";")} }
                |}()""".trimMargin()
            }
        }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer): List<SirBridge> {
            return bridgeProxy.createSirBridges {
                val actualArgs = argNames.drop(1).also { if (extensionReceiverParameter != null) it.drop(1) else it }
                buildCall("(__pointerToBlock as ${typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)}).invoke(${actualArgs.joinToString()})")
            }
        }
    }

    object AsOutError : WithSingleType(swiftType = SirType.never, kotlinType = KotlinType.PointerToKotlinObject, cType = CType.OutObject.nonnulll) {
        override val inKotlinSources: ValueConversion
            get() = IdentityValueConversion

        override val inSwiftSources: ValueConversion
            get() = object : ValueConversion {
                override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                    return "&$valueExpression"
                }

                override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                    return "${typeNamer.swiftFqName(SirNominalType(KotlinRuntimeModule.kotlinBase))}.__createClassWrapper(externalRCRef: $valueExpression)"
                }
            }
    }
}

private fun AnyBridge.renderNil(): String = if (this is AsObjCBridgedOptional) "NSNull()" else "nil"
