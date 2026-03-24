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
import org.jetbrains.kotlin.sir.providers.utils.KotlinCoroutineSupportModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.util.SirPlatformModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.isNever
import org.jetbrains.kotlin.sir.util.isValueType
import org.jetbrains.kotlin.sir.util.name
import org.jetbrains.kotlin.sir.util.swiftName
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

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
    if (type is SirTypedFlowType) return AsTypedFlow(type)
    if (type.protocols.singleOrNull()?.first == KotlinRuntimeSupportModule.kotlinBridgeable) {
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
    is AsOptionalVoid -> AsObjCBridgedOptional(bridge.swiftType)
    is AsObject,
    is AsExistential,
    is AsAnyBridgeable,
    is AsTypedFlow,
    is AsOpaqueObject,
    is SirCustomTypeTranslatorImpl.RangeBridge,
    AsNothing,
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
        SirSwiftModule.never -> AsNothing
        SirSwiftModule.error -> AsObjCBridged(SirSwiftModule.error.nominalType(), CType.NSError)

        SirSwiftModule.optional -> when (val bridge = bridgeType(type.typeArguments.first(), position)) {
            is AsObject,
            is AsObjCBridged,
            is AsExistential,
            is AsAnyBridgeable,
            is AsTypedFlow,
            is AsContravariantBlock,
            is AsCovariantBlock,
            is SirCustomTypeTranslatorImpl.RangeBridge
                -> AsOptionalWrapper(bridge)

            is AsNothing -> AsOptionalNothing
            is AsVoid -> AsOptionalVoid

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
}

internal val SirType.isChar: Boolean
    get() = this is SirNominalType && typeDeclaration == SirSwiftModule.utf16CodeUnit

/**
 * Generate value conversions from Swift to Kotlin.
 */
internal interface SwiftToKotlinValueConversion {
    context(session: SirSession)
    fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String = valueExpression
}

/**
 * Generate value conversions from Kotlin to Swift.
 */
internal interface KotlinToSwiftValueConversion {
    context(session: SirSession)
    fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String = valueExpression
}

/**
 * Generate value conversions between Swift and Kotlin.
 */
internal interface ValueConversion : SwiftToKotlinValueConversion, KotlinToSwiftValueConversion

internal object IdentityValueConversion : ValueConversion {
    context(session: SirSession)
    override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = valueExpression
    context(session: SirSession)
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

    object AsVoid : WithSingleType(SirNominalType(SirSwiftModule.void), KotlinType.Boolean, CType.Bool) {
        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "run<Unit> { $valueExpression }"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "run { ${valueExpression}; true }"
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "{ ${valueExpression}; return true }()"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "{ ${valueExpression}; return () }()"
        }
    }

    object AsOptionalVoid : WithSingleType(
        SirNominalType(SirSwiftModule.optional, listOf(SirNominalType(SirSwiftModule.void))),
        KotlinType.Boolean,
        CType.Bool
    ) {
        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "(if ($valueExpression) Unit else null)"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "($valueExpression != null)"
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "($valueExpression != nil)"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "($valueExpression ? () : nil)"
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
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression) as ${
                    typeNamer.kotlinFqName(
                        swiftType,
                        SirTypeNamer.KotlinNameType.PARAMETRIZED
                    )
                }"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = "${valueExpression}.__externalRCRef()"

            context(session: SirSession)
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
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression) as kotlin.Any"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources: ValueConversion = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(
                typeNamer: SirTypeNamer,
                valueExpression: String,
            ): String {
                return "$valueExpression.__externalRCRef()"
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "${typeNamer.swiftFqName(SirNominalType(KotlinRuntimeModule.kotlinBase))}.__createBridgeable(externalRCRef: $valueExpression)"
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
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression) as ${
                    typeNamer.kotlinFqName(
                        swiftType,
                        SirTypeNamer.KotlinNameType.PARAMETRIZED
                    )
                }"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = "${valueExpression}.__externalRCRef()"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "${typeNamer.swiftFqName(SirNominalType(KotlinRuntimeModule.kotlinBase))}.__createProtocolWrapper(externalRCRef: $valueExpression) as! ${typeNamer.swiftFqName(swiftType)}"
        }
    }

    /**
     * A bridge for the typed `KotlinTypedFlow<Element>` wrapper protocols/structs.
     *
     * On the Kotlin/C side it's the same opaque pointer as for [AsExistential].
     * On the Swift side, the return path wraps the pointer in a `KotlinTypedFlow<Element>` protocol,
     * and the parameter path (theoretical – typed flow only appears in return position)
     * unwraps via `.wrapped.__externalRCRef()`.
     */
    class AsTypedFlow(swiftType: SirTypedFlowType) : WithSingleType(swiftType, KotlinType.KotlinObject, CType.Object) {

        private val structType = SirNominalType(
            typeDeclaration = when (swiftType.typedProtocol) {
                KotlinCoroutineSupportModule.kotlinTypedFlow -> KotlinCoroutineSupportModule.kotlinTypedFlowImpl
                KotlinCoroutineSupportModule.kotlinTypedSharedFlow -> KotlinCoroutineSupportModule.kotlinTypedSharedFlowImpl
                KotlinCoroutineSupportModule.kotlinTypedMutableSharedFlow -> KotlinCoroutineSupportModule.kotlinTypedMutableSharedFlowImpl
                KotlinCoroutineSupportModule.kotlinTypedStateFlow -> KotlinCoroutineSupportModule.kotlinTypedStateFlowImpl
                KotlinCoroutineSupportModule.kotlinTypedMutableStateFlow -> KotlinCoroutineSupportModule.kotlinTypedMutableStateFlowImpl
                else -> error("Unsupported typed flow type: ${swiftType.typedProtocol}")
            },
            typeArguments = listOf(swiftType.elementType)
        )

        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression) as ${
                    typeNamer.kotlinFqName(
                        swiftType,
                        SirTypeNamer.KotlinNameType.PARAMETRIZED)}"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "${valueExpression}.wrapped.__externalRCRef()"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val kotlinBaseName = typeNamer.swiftFqName(SirNominalType(KotlinRuntimeModule.kotlinBase))
                val flowProtocolFqName = typeNamer.swiftFqName(swiftType.flowType)
                val structFqName = typeNamer.swiftFqName(structType)
                return "$structFqName($kotlinBaseName.__createProtocolWrapper(externalRCRef: $valueExpression) as! $flowProtocolFqName)"
            }
        }
    }

    class AsOpaqueObject(swiftType: SirType, kotlinType: KotlinType, cType: CType) : WithSingleType(swiftType, kotlinType, cType) {
        override val inKotlinSources = object : ValueConversion {
            // nulls are handled by AsOptionalWrapper, so safe to cast from nullable to non-nullable
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression)!!"

            context(session: SirSession)
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
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "interpretObjCPointer<${typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)}>($valueExpression)"

            context(session: SirSession)
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
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                return "$valueExpression as! NSObject? ?? NSNull()"
            }

            context(session: SirSession)
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
                context(session: SirSession)
                override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                    "interpretObjCPointer<Int>($valueExpression).toChar()"

                context(session: SirSession)
                override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                    "$valueExpression.objcPtr()"
            }
        } else {
            super.inKotlinSources
        }


        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "NSNumber(value: $valueExpression)"

            context(session: SirSession)
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
            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                super@OptionalChar.inKotlinSources.kotlinToSwift(typeNamer, "${valueExpression}?.code")
        }
    }

    class AsNSObject(
        swiftType: SirNominalType,
    ) : AsObjCBridged(swiftType, CType.NSObject) {
        override val inSwiftSources: ValueConversion = object : ValueConversion {
            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "$valueExpression as! ${typeNamer.swiftFqName(swiftType)}"
        }
    }

    abstract class AsNSCollection(
        swiftType: SirNominalType,
        cType: CType,
    ) : AsObjCBridged(swiftType, cType) {
        abstract inner class InSwiftSources : ValueConversion {
            context(session: SirSession)
            abstract override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                return "$valueExpression as! ${typeNamer.swiftFqName(swiftType)}"
            }
        }
    }

    open class AsNSArray(swiftType: SirNominalType, elementBridge: WithSingleType) : AsNSCollection(swiftType, CType.NSArray(elementBridge.cType)) {
        override val inSwiftSources = object : InSwiftSources() {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                return valueExpression.mapSwift { elementBridge.inSwiftSources.swiftToKotlin(typeNamer, it) }
            }
        }
    }

    class AsNSArrayForVariadic(swiftType: SirNominalType, elementBridge: WithSingleType) : AsNSArray(swiftType, elementBridge) {
        override val inKotlinSources: ValueConversion = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val arrayKind = typeNamer.kotlinPrimitiveFqNameIfAny(swiftType.typeArguments.single()) ?: "Typed"
                return "interpretObjCPointer<${
                    typeNamer.kotlinFqName(
                        swiftType,
                        SirTypeNamer.KotlinNameType.PARAMETRIZED
                    )
                }>($valueExpression).to${arrayKind}Array()"
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "$valueExpression.objcPtr()"
        }
    }

    class AsNSSet(swiftType: SirNominalType, elementBridge: WithSingleType) : AsNSCollection(swiftType, CType.NSSet(elementBridge.cType)) {
        override val inSwiftSources = object : InSwiftSources() {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val transformedElements = valueExpression.mapSwift { elementBridge.inSwiftSources.swiftToKotlin(typeNamer, it) }
                return if (transformedElements == valueExpression) valueExpression else "Set($transformedElements)"
            }
        }
    }

    class AsNSDictionary(swiftType: SirNominalType, val keyBridge: WithSingleType, val valueBridge: WithSingleType) :
        AsNSCollection(swiftType, CType.NSDictionary(keyBridge.cType, valueBridge.cType)) {

        override val inSwiftSources = object : InSwiftSources() {
            context(session: SirSession)
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

    data object AsNothing : WithSingleType(
        SirNominalType(SirSwiftModule.never),
        KotlinType.Boolean,
        CType.Bool
    ) {
        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "run { ${valueExpression}; throw IllegalStateException() }"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String = valueExpression
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "{ $valueExpression }()"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "{ ${valueExpression}; fatalError() }()"
        }
    }

    data object AsOptionalNothing : WithSingleType(
        SirNominalType(SirSwiftModule.optional, listOf(SirNominalType(SirSwiftModule.never))),
        KotlinType.Boolean,
        CType.Bool
    ) {
        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "run { ${valueExpression}; null }"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "run { ${valueExpression}; true }"
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "{ ${valueExpression}; return true }()"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "{ ${valueExpression}; return nil }()"
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
                context(session: SirSession)
                override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                    require(wrappedObject is SwiftToKotlinBridge)
                    return "if ($valueExpression == kotlin.native.internal.NativePtr.NULL) null else ${
                        wrappedObject.inKotlinSources.swiftToKotlin(typeNamer, valueExpression)
                    }"
                }

                context(session: SirSession)
                override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                    require(wrappedObject is KotlinToSwiftBridge)
                    return "if ($valueExpression == null) kotlin.native.internal.NativePtr.NULL else ${
                        wrappedObject.inKotlinSources.kotlinToSwift(typeNamer, valueExpression)
                    }"
                }
            }

        override val inSwiftSources: ValueConversion = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                require(
                    wrappedObject is AsObjCBridged || wrappedObject is AsObject ||
                            wrappedObject is AsExistential || wrappedObject is AsAnyBridgeable || wrappedObject is AsTypedFlow ||
                            wrappedObject is AsContravariantBlock ||
                            wrappedObject is SirCustomTypeTranslatorImpl.RangeBridge
                )
                return valueExpression.mapSwift { wrappedObject.inSwiftSources.swiftToKotlin(typeNamer, it) } +
                        " ?? ${wrappedObject.renderNil()}"
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                return when (wrappedObject) {
                    is AsObjCBridged, is AsCovariantBlock ->
                        valueExpression.mapSwift { wrappedObject.inSwiftSources.kotlinToSwift(typeNamer, it) }
                    is AsObject, is AsExistential, is AsAnyBridgeable, is AsTypedFlow, is SirCustomTypeTranslatorImpl.RangeBridge ->
                        "{ switch $valueExpression { case ${wrappedObject.renderNil()}: .none; case let res: ${
                            wrappedObject.inSwiftSources.kotlinToSwift(typeNamer, "res")
                        }; } }()"
                    is AsContravariantBlock,
                    is AsIs,
                    is AsOpaqueObject,
                    is AsOutError,
                        -> TODO("not yet supported")

                    is AsNothing -> error("AsOptionalNothing must be used for AsNothing")
                    is AsVoid -> error("AsOptionalVoid must be used for AsVoid")
                    is AsOptionalWrapper, AsOptionalNothing, AsOptionalVoid -> error("there is not optional wrappers for optional")
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
        private val contextParameters: List<KotlinToSwiftBridge>,
        private val parameters: List<KotlinToSwiftBridge>,
        private val returnType: SwiftToKotlinBridge,
        private val session: SirSession,
        private val asyncParameters: Triple<KotlinToSwiftBridge, KotlinToSwiftBridge, KotlinToSwiftBridge>?,
    ) : SwiftToKotlinBridgeWithSingleType(
        swiftType = swiftType,
        kotlinType = KotlinType.KotlinObject,
        cType = CType.BlockPointer(
            // TODO: think about it, as types like ranges seems possible here making first() call illegal (?)
            parameters = (contextParameters + parameters).map { it.typeList.first().cType } +
                    (asyncParameters?.toList()?.map { it.typeList.first().cType } ?: emptyList()),
            returnType = asyncParameters?.let { CType.Void } ?: returnType.typeList.first().cType,
        )
    ) {
        private val kotlinFunctionTypeRendered = buildString {
            append("(")
            append((contextParameters + parameters + (asyncParameters?.toList() ?: emptyList())).joinToString { it.typeList.single().kotlinType.repr })
            append(")->")
            append(asyncParameters?.let { "Unit" } ?: returnType.typeList.single().kotlinType.repr)
        }

        companion object {
            context(session: SirSession)
            private fun computeAsyncParameters(
                returnType: SwiftToKotlinBridge
            ): Triple<KotlinToSwiftBridge, KotlinToSwiftBridge, KotlinToSwiftBridge> = Triple(
                // continuation
                AsCovariantBlock(parameters = listOf(returnType), returnType = AsVoid),
                // exception
                AsCovariantBlock(
                    parameters = listOf(AsObjCBridged(SirSwiftModule.error.nominalType(), CType.NSError)),
                    returnType = AsVoid,
                ),
                // cancellation
                AsObject(
                    swiftType = KotlinCoroutineSupportModule.swiftJob.nominalType(),
                    kotlinType = KotlinType.KotlinObject,
                    cType = CType.Object,
                ),
            )

            context(session: SirSession)
            operator fun invoke(
                swiftType: SirFunctionalType,
            ): AsContravariantBlock {
                val parameters = swiftType.parameterTypes.map { bridgeReturnType(it) }
                val contextParameters = swiftType.contextTypes.map { bridgeReturnType(it) }
                val returnType = bridgeParameterType(swiftType.returnType)
                val asyncParameters = swiftType.isAsync.ifTrue { computeAsyncParameters(returnType) }

                return AsContravariantBlock(
                    swiftType,
                    contextParameters,
                    parameters,
                    returnType,
                    session,
                    asyncParameters,
                )
            }

            context(session: SirSession)
            operator fun invoke(
                parameters: List<KotlinToSwiftBridge>,
                returnType: SwiftToKotlinBridge,
                isAsync: Boolean = false,
            ): AsContravariantBlock {
                val swiftType = SirFunctionalType(
                    parameterTypes = parameters.map { it.swiftType.escaping },
                    isAsync = isAsync,
                    returnType = returnType.swiftType,
                )
                val asyncParameters = swiftType.isAsync.ifTrue { computeAsyncParameters(returnType) }
                return AsContravariantBlock(
                    swiftType,
                    emptyList(),
                    parameters,
                    returnType,
                    session,
                    asyncParameters,
                )
            }
        }

        override val inKotlinSources: SwiftToKotlinValueConversion
            get() = object : ValueConversion {
                context(session: SirSession)
                override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String = if (swiftType.isAsync) {
                    asyncBlockSwiftToKotlin(typeNamer, valueExpression)
                } else {
                    syncBlockSwiftToKotlin(typeNamer, valueExpression)
                }

                private val argsInClosure: List<Pair<String, KotlinToSwiftBridge>>? get() = buildList {
                        addAll(contextParameters.mapIndexed { idx, el -> "ctx${idx}" to el })
                        addAll(parameters.mapIndexed { idx, el -> "arg${idx}" to el })
                    }.takeIf { it.isNotEmpty() }

                private fun defineArgs(typeNamer: SirTypeNamer, argsInClosure: List<Pair<String, KotlinToSwiftBridge>>?): String? =
                    argsInClosure?.let { args ->
                        " ${
                            args.joinToString { (name, bridge) ->
                                "$name: ${typeNamer.kotlinFqName(bridge.swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)}"
                            }
                        } ->"
                    }

                private fun syncBlockSwiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String = with(session) {
                    val argsInClosure = argsInClosure
                    val defineArgs = defineArgs(typeNamer, argsInClosure) ?: ""
                    val callArgs = argsInClosure
                        ?.joinToString { (name, bridge) -> bridge.inKotlinSources.kotlinToSwift(typeNamer, name) } ?: ""
                    return@with """run {    
                    |    val kotlinFun = convertBlockPtrToKotlinFunction<$kotlinFunctionTypeRendered>($valueExpression);
                    |    {$defineArgs
                    |        val _result = kotlinFun($callArgs)
                    |        ${returnType.inKotlinSources.swiftToKotlin(typeNamer, "_result")} 
                    |    }
                    |}""".replaceIndentByMargin("    ")
                }

                private fun asyncBlockSwiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String = with(session) {
                    val (continuationBridge, exceptionBridge, cancellationBridge) = asyncParameters
                        ?: error("Async parameters must be present for an async function")

                    val argsInClosure = argsInClosure
                    val defineArgs = defineArgs(typeNamer, argsInClosure) ?: ""

                    val callArgs = buildString {
                        argsInClosure?.let { args ->
                            append(args.joinToString { (name, bridge) ->
                                bridge.inKotlinSources.kotlinToSwift(typeNamer, name)
                            })
                            append(", ")
                        }
                        append("__continuationPtr, __exceptionPtr, __cancellationPtr")
                    }

                    val continuationKotlinType = typeNamer.kotlinFqName(
                        continuationBridge.swiftType,
                        SirTypeNamer.KotlinNameType.PARAMETRIZED
                    )

                    val exceptionKotlinType = typeNamer.kotlinFqName(
                        exceptionBridge.swiftType,
                        SirTypeNamer.KotlinNameType.PARAMETRIZED
                    )

                    val cancellationKotlinType = typeNamer.kotlinFqName(
                        cancellationBridge.swiftType,
                        SirTypeNamer.KotlinNameType.PARAMETRIZED
                    )

                    val cancellationPtrConversion = cancellationBridge.inKotlinSources.kotlinToSwift(typeNamer, "__cancellation")
                    val continuationPtrConversion = continuationBridge.inKotlinSources.kotlinToSwift(typeNamer, "__continuation")
                    val exceptionPtrConversion = exceptionBridge.inKotlinSources.kotlinToSwift(typeNamer, "__exception")

                    return@with """run {
                    |    val originalBlock = convertBlockPtrToKotlinFunction<$kotlinFunctionTypeRendered>($valueExpression);
                    |    suspend {$defineArgs
                    |        val __cancellation: $cancellationKotlinType = SwiftJob()
                    |        kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]?.let { 
                    |            __cancellation.alsoCancel(it)
                    |            it.alsoCancel(__cancellation)
                    |        }
                    |        
                    |        kotlinx.coroutines.suspendCancellableCoroutine { __cont ->
                    |            val __cancellationPtr = $cancellationPtrConversion
                    |            val __continuation: $continuationKotlinType = { _result ->
                    |                __cont.resumeWith(kotlin.Result.success(_result))
                    |            }
                    |            val __continuationPtr = $continuationPtrConversion
                    |            val __exception: $exceptionKotlinType = { _error ->
                    |                __cont.resumeWith(kotlin.Result.failure(SwiftException(_error)))
                    |            }
                    |            val __exceptionPtr = $exceptionPtrConversion
                    |            originalBlock($callArgs)
                    |        }
                    |    }
                    |}""".replaceIndentByMargin("    ")
                }
            }

        override val inSwiftSources = object : SwiftToKotlinValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String = if (swiftType.isAsync) {
                return asyncSwiftToKotlin(typeNamer, valueExpression)
            } else {
                return syncSwiftToKotlin(typeNamer, valueExpression)
            }

            private fun syncSwiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String = with(session) {
                val contextArgs = contextParameters.mapIndexed { idx, el -> "ctx${idx}" to el }
                val regularArgs = parameters.mapIndexed { idx, el -> "arg${idx}" to el }
                val defineArgs = (contextArgs + regularArgs).takeIf { it.isNotEmpty() }?.let { " ${it.joinToString { it.first }} in" } ?: ""
                val callContextArg = contextArgs.map { param ->
                    param.second.inSwiftSources.kotlinToSwift(typeNamer, param.first)
                }.takeIf { it.isNotEmpty() }?.joinToString(separator = ",", prefix = "(", postfix = ")")
                val callRegularArgs = regularArgs.map { param ->
                    param.second.inSwiftSources.kotlinToSwift(typeNamer, param.first)
                }
                val callArgs = (listOfNotNull(callContextArg) + callRegularArgs).takeIf { it.isNotEmpty() }?.joinToString() ?: ""

                val callSite = returnType.inSwiftSources.swiftToKotlin(typeNamer, "originalBlock($callArgs)")

                return@with """{
                |    let originalBlock = $valueExpression
                |    return {$defineArgs return $callSite }
                |}()""".trimMargin()
            }

            private fun asyncSwiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String = with(session) {
                val (continuationBridge, exceptionBridge, cancellationBridge) = asyncParameters ?: error("Async parameters must present for an async function ")

                val regularArgsInClosure = parameters
                    .mapIndexed { idx, el -> "arg${idx}" to el }.takeIf { it.isNotEmpty() }

                val regularArgNames = regularArgsInClosure?.joinToString { it.first } ?: ""
                val defineArgs = if (regularArgNames.isNotEmpty()) {
                    " $regularArgNames, __continuationPtr, __exceptionPtr, __cancellationPtr in"
                } else {
                    " __continuationPtr, __exceptionPtr, __cancellationPtr in"
                }

                val argsConverisons = regularArgsInClosure?.map { (argName, bridge) ->
                    Triple(
                        "__wrapped_${argName}",
                        typeNamer.swiftFqName(bridge.swiftType),
                        bridge.inSwiftSources.kotlinToSwift(typeNamer, argName)
                    )
                }

                val originalBlockCallArgs = argsConverisons?.joinToString { it.first } ?: ""

                val argsBindings = argsConverisons?.joinToString("\n") { (name, type, expression) ->
                    "let $name: $type = $expression"
                } ?: ""

                val continuationSwiftConversion = continuationBridge.inSwiftSources.kotlinToSwift(typeNamer, "__continuationPtr")
                val exceptionSwiftConversion = exceptionBridge.inSwiftSources.kotlinToSwift(typeNamer, "__exceptionPtr")
                val cancellationSwiftConversion = cancellationBridge.inSwiftSources.kotlinToSwift(typeNamer, "__cancellationPtr")

                val continuationSwiftType = typeNamer.swiftFqName(continuationBridge.swiftType)
                val exceptionSwiftType = typeNamer.swiftFqName(exceptionBridge.swiftType)
                val cancellationSwiftType = typeNamer.swiftFqName(cancellationBridge.swiftType)

                return@with """{
                |    let originalBlock = $valueExpression
                |    return {$defineArgs
                |        let __continuation: $continuationSwiftType = $continuationSwiftConversion
                |        let __exception: $exceptionSwiftType = $exceptionSwiftConversion
                |        let __cancellation: $cancellationSwiftType = $cancellationSwiftConversion
                |        ${argsBindings.prependIndent("        ")}
                |        let task = Task {
                |            await withTaskCancellationHandler {
                |                do {
                |                    let result = try await originalBlock($originalBlockCallArgs)
                |                    __continuation(result)
                |                } catch {
                |                    __exception(error)
                |                }
                |            } onCancel: {
                |                __cancellation.cancelExternally()
                |            }
                |        }
                |        __cancellation.setCallback { shouldCancel in
                |            defer { if shouldCancel { task.cancel() } }
                |            return task.isCancelled
                |        }
                |    }
                |}()""".trimMargin()
            }
        }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer): List<SirBridge> {
            val baseHelpers = parameters.flatMap { it.helperBridges(typeNamer) } + returnType.helperBridges(typeNamer)
            val asyncHelpers = asyncParameters?.let { (continuation, exception, cancellation) ->
                continuation.helperBridges(typeNamer) + exception.helperBridges(typeNamer) + cancellation.helperBridges(typeNamer)
            } ?: emptyList()
            return baseHelpers + asyncHelpers
        }
    }

    class AsCovariantBlock private constructor(
        override val swiftType: SirFunctionalType,
        private val bridgeProxy: BridgeFunctionProxy?,
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
                    ) + swiftType.contextTypes.mapIndexed { idx, type ->
                        SirParameter(argumentName = "ctx${idx}", type = type)
                    } + swiftType.parameterTypes.map { SirParameter(type = it) },
                    returnType = swiftType.returnType,
                    kotlinFqName = FqName(""),
                    selfParameter = null,
                    contextParameters = emptyList(),
                    extensionReceiverParameter = null,
                    errorParameter = null,
                    isAsync = swiftType.isAsync
                )
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
                context(session: SirSession)
                override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                    "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
            }

        override val inSwiftSources = object : KotlinToSwiftValueConversion {
            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val allArgs = bridgeProxy?.argumentsForInvocation()?.applyIf(swiftType.isAsync) { dropLast(3) }
                    ?: List(1 + swiftType.contextTypes.size + swiftType.parameterTypes.size) { "_" }
                val defineArgs = buildList {
                    if (swiftType.contextType != null) add("context")
                    addAll(allArgs.drop(1 + swiftType.contextTypes.size))
                }.takeIf { it.isNotEmpty() }?.let { " ${it.joinToString()} in" } ?: ""
                val swiftInvocation = buildList {
                    if (swiftType.contextType != null) {
                        add(List(swiftType.contextTypes.size) { idx -> "ctx$idx" }.joinToString(prefix = "let (", postfix = ") = context"))
                    }
                    if (bridgeProxy != null) {
                        addAll(bridgeProxy.createSwiftInvocation({ "return $it" }))
                    } else {
                        add("fatalError()")
                    }
                }
                return """{
                |    let ${allArgs.first()} = $valueExpression
                |    return {$defineArgs ${swiftInvocation.joinToString(";")} }
                |}()""".trimMargin()
            }
        }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer): List<SirBridge> {
            return bridgeProxy?.createSirBridges {
                val actualArgs = argNames.drop(1).also { if (extensionReceiverParameter != null) it.drop(1) else it }
                buildCall("(__pointerToBlock as ${typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)}).invoke(${actualArgs.joinToString()})")
            } ?: emptyList()
        }
    }

    object AsOutError : WithSingleType(swiftType = SirType.never, kotlinType = KotlinType.PointerToKotlinObject, cType = CType.OutObject.nonnulll) {
        override val inKotlinSources: ValueConversion
            get() = IdentityValueConversion

        override val inSwiftSources: ValueConversion
            get() = object : ValueConversion {
                context(session: SirSession)
                override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                    return "&$valueExpression"
                }

                context(session: SirSession)
                override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                    return "${typeNamer.swiftFqName(SirNominalType(KotlinRuntimeModule.kotlinBase))}.__createClassWrapper(externalRCRef: $valueExpression)"
                }
            }
    }
}

private fun AnyBridge.renderNil(): String = if (this is AsObjCBridgedOptional) "NSNull()" else "nil"
