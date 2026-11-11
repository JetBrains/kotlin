/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.BridgeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.isMarkedNullable
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTranslationResult
import org.jetbrains.kotlin.sir.providers.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.*
import org.jetbrains.kotlin.sir.providers.sirModule
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.toBridge
import org.jetbrains.kotlin.sir.providers.toSir
import org.jetbrains.kotlin.sir.providers.utils.KotlinCoroutineSupportModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.utils.allRequiredOptIns
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.SirPlatformLikeModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.containsTypeParameter
import org.jetbrains.kotlin.sir.util.expandedType
import org.jetbrains.kotlin.sir.util.isValueType
import org.jetbrains.kotlin.sir.util.name
import org.jetbrains.kotlin.sir.util.substituteTypeParameters
import org.jetbrains.kotlin.sir.util.swiftIdentifier
import org.jetbrains.kotlin.sir.util.swiftName
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

private val rawNullablePointerSirType: SirType
    get() = SirSwiftModule.unsafeMutableRawPointer.nominalType().optional()

private fun SirFunctionalType.eraseGenericPositions(): SirFunctionalType {
    if (!this.containsTypeParameter()) return this
    return SirFunctionalType(
        contextTypes = contextTypes.map { if (it.containsTypeParameter()) rawNullablePointerSirType else it },
        parameterTypes = parameterTypes.map { if (it.containsTypeParameter()) rawNullablePointerSirType else it },
        isAsync = isAsync,
        errorType = errorType,
        returnType = if (returnType.containsTypeParameter()) rawNullablePointerSirType else returnType,
        origin = origin,
        attributes = attributes,
    )
}

private val SirFunctionalType.bridgeNameSuffix: String
    get() = (if (isAsync) "async_" else "") +
            (if (contextTypes.isNotEmpty()) "ctx${contextTypes.size}_" else "") +
            eraseGenericPositions().returnType.swiftName

context(session: SirSession)
internal fun bridgeType(type: SirType): BidirectionalBridge =
    bridgeType(type, SirTypeVariance.INVARIANT) as BidirectionalBridge

context(session: SirSession)
internal fun bridgeParameterType(type: SirType): SwiftToKotlinBridge =
    bridgeType(type, SirTypeVariance.CONTRAVARIANT) as SwiftToKotlinBridge

context(session: SirSession)
internal fun bridgeReturnType(type: SirType): KotlinToSwiftBridge =
    bridgeType(type, SirTypeVariance.COVARIANT) as KotlinToSwiftBridge

context(session: SirSession)
internal fun asyncContinuationBridges(
    resultBridge: SwiftToKotlinBridge,
): Triple<KotlinToSwiftBridge, KotlinToSwiftBridge, KotlinToSwiftBridge> = Triple(
    // continuation
    AsInvariantBlock(parameters = listOf(resultBridge), returnType = AsVoid),
    // exception
    // A `null` exception is used to signal a cancellation, see `suspendSwiftCoroutine`.
    AsInvariantBlock(
        parameters = listOf(AsOptionalWrapper(AsError())),
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
internal fun renderKotlinSuspendSwiftCoroutine(
    typeNamer: SirTypeNamer,
    async: Triple<KotlinToSwiftBridge, KotlinToSwiftBridge, KotlinToSwiftBridge>,
    prelude: String = "",
    call: (continuation: String, exception: String, cancellation: String) -> String,
): String {
    val args = listOf(
        "continuation" to async.first,
        "exception" to async.second,
        "cancellation" to async.third,
    )
    val defineArgs = args.joinToString(prefix = " ", postfix = " ->") {
        "${it.first}: ${typeNamer.kotlinFqName(it.second.swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)}"
    }
    val lines = buildList {
        if (prelude.isNotEmpty()) add(prelude)
        args.forEach { add("val _${it.first} = ${it.second.inKotlinSources.kotlinToSwift(typeNamer, it.first)}") }
        add(call("_continuation", "_exception", "_cancellation"))
    }
    return "suspendSwiftCoroutine {$defineArgs\n${lines.joinToString("\n").prependIndent("    ")}\n}"
}

context(session: SirSession)
private fun bridgeTypeForVariadicParameter(type: SirType): Bridge =
    AsNSArrayForVariadic(SirArrayType(type), bridgeAsNSCollectionElement(type))

context(session: SirSession)
internal fun bridgeType(type: SirType, position: SirTypeVariance): Bridge =
    bridgeReifiedType(type, position) ?: when (type) {
        is SirTypeParameterType -> AsBridgeableValue(type)
        is SirNominalType -> bridgeNominalType(type, position)
        is SirExistentialType -> bridgeExistential(type, position)
        is SirFunctionalType -> AsInvariantBlock(type)
        else -> error("Attempt to bridge unbridgeable type: $type.")
    }

context(session: SirSession)
private fun bridgeReifiedType(type: SirType, position: SirTypeVariance): Bridge? {
    val origin = type.origin as? SirType.Origin.ReifiedType ?: return null
    val untypedBridge = bridgeType(origin.erasedType, position)
    check(untypedBridge is BidirectionalBridge) { "Reified type $type requires a BidirectionalBridge" }
    return when (origin) {
        is SirType.Origin.ReifiedType.Flow -> AsTyped.Flow(origin, untypedBridge)
    }
}

context(session: SirSession)
private fun bridgeExistential(type: SirExistentialType, position: SirTypeVariance): Bridge {
    val singleProto = type.protocols.singleOrNull()
    if (singleProto?.first == KotlinRuntimeSupportModule.kotlinBridgeable) {
        return AsAnyBridgeable
    }
    if (singleProto?.first?.parent is SirPlatformLikeModule) {
        return AsObjCProtocol(type)
    }
    if (singleProto != null) {
        val anyWrapper = singleProto.first.kaSymbolOrNull<KaDeclarationSymbol>()?.let { sym ->
            session.withSessions {
                (sym.toSir() as? SirTranslationResult.RegularInterface)?.anyWrapperDeclaration
            }
        }
        if (anyWrapper != null) {
            val wrapperArgs = when {
                singleProto.first.typeParameters.isEmpty() -> emptyList()
                singleProto.second.isNotEmpty() -> singleProto.second
                else -> singleProto.first.typeParameters.map { concreteTypeForExistentialWrapperArg(it) }
            }
            return AsPatExistential(type, SirNominalType(anyWrapper, wrapperArgs))
        }
    }
    return AsExistential(type)
}

context(session: SirSession)
internal fun concreteTypeForExistentialWrapperArg(tp: SirTypeParameter): SirType {
    val nonTrivial = tp.bounds.singleOrNull { bound ->
        val decl = (bound as? SirNominalType)?.typeDeclaration
        decl != KotlinRuntimeSupportModule.kotlinBridgeable && decl != SirSwiftModule.hashable
    }
    if (nonTrivial is SirNominalType) {
        when (val decl = nonTrivial.typeDeclaration) {
            is SirClass -> return nonTrivial
            is SirProtocol -> {
                val res = decl.kaSymbolOrNull<KaDeclarationSymbol>()?.let { sym ->
                    session.withSessions { sym.toSir() as? SirTranslationResult.RegularInterface }
                }
                if (res != null) {
                    val subAny = res.anyWrapperDeclaration
                    return if (subAny != null) {
                        SirNominalType(subAny, decl.typeParameters.map { concreteTypeForExistentialWrapperArg(it) })
                    } else {
                        SirNominalType(
                            KotlinRuntimeSupportModule.kotlinExistential,
                            listOf(SirExistentialType(res.markerDeclaration)),
                        )
                    }
                }
            }
            else -> {}
        }
    }
    val needsHashable = tp.bounds.any { (it as? SirNominalType)?.typeDeclaration == SirSwiftModule.hashable }
    return if (needsHashable) {
        SirType.anyHashable.optional()
    } else {
        SirNominalType(KotlinRuntimeSupportModule.kotlinAny).optional()
    }
}

context(session: SirSession)
internal fun bridgeAsNSCollectionElement(type: SirType): BidirectionalBridge = when (val bridge = bridgeType(type)) {
    is AsIs -> AsNSNumber(bridge.swiftType)
    is AsOptionalWrapper -> when (bridge.wrappedObject) {
        is AsInvariantBlock -> AsBlockPointerInCollection(bridge.wrappedObject, bridge.swiftType)
        else -> AsObjCBridgedOptional(bridge.wrappedObject.swiftType)
    }
    is AsOptionalNothing -> AsObjCBridgedOptional(bridge.swiftType)
    is AsOptionalVoid,
    is AsNestedOptionalVoid,
        -> AsObjCBridgedOptional(bridge.swiftType)
    is AsObject,
    is AsGenericObject,
    is AsPatExistential,
    is AsRetainedPatExistential,
    is AsRetainedTypedFlow,
    is AsRetainedBlock,
    is AsBridgeableValue,
    is AsRawNullableBridgePtr,
    is AsExistential,
    is AsAnyBridgeable,
    is AsTyped,
    is AsOpaqueObject,
    is SirCustomTypeTranslatorImpl.RangeBridge,
    AsNothing,
        -> AsObjCBridged(bridge.swiftType, CType.id)
    is AsInvariantBlock -> AsBlockPointerInCollection(bridge)
    is AsBlockPointerInCollection,
    is AsObjCBridged,
    AsOutError,
    is AsError,
    AsVoid,
        -> bridge
}

context(session: SirSession)
private fun bridgeNominalType(type: SirNominalType, position: SirTypeVariance): Bridge {
    val customTypeBridgeWrapper = type.toBridge()
    if (customTypeBridgeWrapper != null) return customTypeBridgeWrapper.bridge
    return when (val subtype = type.typeDeclaration) {
        SirSwiftModule.unsafeMutableRawPointer -> AsOpaqueObject(type, KotlinType.KotlinObject, CType.Object)
        SirSwiftModule.never -> AsNothing
        SirSwiftModule.error -> AsError()
        KotlinRuntimeSupportModule.kotlinUnit,
        KotlinRuntimeSupportModule.kotlinAny,
            -> AsBridgeableValue(type)

        SirSwiftModule.optional -> {
            val wrappedArg = type.typeArguments.first()
            if (wrappedArg is SirNominalType && wrappedArg.typeDeclaration == SirSwiftModule.unsafeMutableRawPointer) {
                return AsRawNullableBridgePtr(type)
            }
            when (val bridge = bridgeType(wrappedArg, position)) {
                is AsBridgeableValue -> AsBridgeableValue(type)
                is AsOptionalNothing -> AsBridgeableValue(type)
                is AsOptionalVoid -> AsNestedOptionalVoid(type)
                is AsOptionalWrapper,
                is AsObject,
                is AsGenericObject,
                is AsPatExistential,
                is AsRetainedPatExistential,
                is AsRetainedTypedFlow,
                is AsRetainedBlock,
                is AsObjCBridged,
                is AsExistential,
                is AsAnyBridgeable,
                is AsTyped,
                is AsInvariantBlock,
                is AsError,
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
        }

        is SirTypealias -> {
            val expanded = if (subtype.typeParameters.isNotEmpty() && type.typeArguments.size == subtype.typeParameters.size) {
                subtype.type.substituteTypeParameters(subtype.typeParameters.zip(type.typeArguments).toMap())
            } else {
                subtype.type
            }
            bridgeType(expanded, position)
        }

        // TODO: Right now, we just assume everything nominal that we do not recognize is a class. We should make this decision looking at kotlin type?
        else -> if (type.typeDeclaration.parent is SirPlatformLikeModule) {
            AsNSObject(type)
        } else if (subtype is SirClass && (subtype.typeParameters.isNotEmpty() || type.typeArguments.isNotEmpty())) {
            AsGenericObject(type)
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
        override val bridge: BidirectionalBridge,
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


internal sealed interface KotlinToSwiftBridge : Bridge {
    val kotlinType: KotlinType
    val cType: CType
    val inKotlinSources: KotlinToSwiftValueConversion
    val inSwiftSources: KotlinToSwiftValueConversion
}

internal sealed interface SwiftToKotlinBridge : Bridge {
    val kotlinType: KotlinType
    val cType: CType
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
internal sealed interface BidirectionalBridge : SwiftToKotlinBridge, KotlinToSwiftBridge {
    override val inKotlinSources: ValueConversion
    override val inSwiftSources: ValueConversion
}

internal sealed interface Bridge {
    val swiftType: SirType

    context(session: SirSession)
    fun kotlinReverseParameterTypeFqName(typeNamer: SirTypeNamer): String =
        kotlinMultiBoundReverseParameterTypeFqNameOrNull(swiftType)
            ?: typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)

    context(sir: SirSession)
    fun helperBridges(typeNamer: SirTypeNamer, position: SirTypeVariance): List<SirBridge> = emptyList()

    /**
     * A bridge that performs an as-is (trivial) conversion.
     *
     * This type of bridge is primarily used for primitive types (integers, floating-point numbers, booleans, and unicode units).
     * No conversions are performed here: values are represented identically
     * across all three languages (Kotlin, Swift, and Objective-C).
     *
     */
    class AsIs(override val swiftType: SirType, override val kotlinType: KotlinType, override val cType: CType) : BidirectionalBridge {
        constructor(swiftDeclaration: SirScopeDefiningDeclaration, kotlinType: KotlinType, cType: CType) : this(
            SirNominalType(swiftDeclaration), kotlinType, cType
        )

        override val inKotlinSources = IdentityValueConversion
        override val inSwiftSources = IdentityValueConversion
    }

    object AsVoid : BidirectionalBridge {
        override val swiftType = SirNominalType(SirSwiftModule.void)
        override val kotlinType = KotlinType.Boolean
        override val cType = CType.Bool
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

    object AsOptionalVoid : BidirectionalBridge {
        override val swiftType = SirNominalType(SirSwiftModule.optional, listOf(SirNominalType(SirSwiftModule.void)))
        override val kotlinType = KotlinType.Boolean
        override val cType = CType.Bool
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

    class AsNestedOptionalVoid(override val swiftType: SirNominalType) : BidirectionalBridge {
        override val kotlinType = KotlinType.Boolean
        override val cType = CType.Bool
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
                "(($valueExpression ?? nil) != nil)"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "($valueExpression ? .some(()) : .none)"
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
    class AsObject(override val swiftType: SirNominalType, override val kotlinType: KotlinType, override val cType: CType) : BidirectionalBridge {
        private val isKotlinEnum: Boolean
            get() = when (val decl = swiftType.typeDeclaration) {
                is SirEnum -> true
                is SirTypealias -> (decl.expandedType as? SirNominalType)?.typeDeclaration is SirEnum
                else -> false
            }

        override val inKotlinSources = object : ValueConversion {
            // nulls are handled by AsOptionalWrapper, so safe to cast from nullable to non-nullable
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val ktTypeName = typeNamer.kotlinFqName(
                    swiftType,
                    SirTypeNamer.KotlinNameType.PARAMETRIZED
                )
                return if (isKotlinEnum) {
                    "(dereferenceAndDisposeBridgeableRef($valueExpression) as $ktTypeName)"
                } else {
                    "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression) as $ktTypeName"
                }
            }

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
                return if (isKotlinEnum) {
                    "$swiftFqName.__fromKotlinPointer($valueExpression)"
                } else if (swiftType.isValueType) {
                    "$swiftFqName(__externalRCRefUnsafe: $valueExpression, options: .asBestFittingWrapper)"
                } else {
                    "$swiftFqName.__createClassWrapper(externalRCRef: $valueExpression)"
                }
            }
        }
    }

    /**
     * Symmetric `+1` retained `_KotlinBridgeable` PWT witness bridge for generic type parameters (`T`),
     * `Optional<T>`, `KotlinUnit`, `KotlinAny`, and generic collections (`[T]`, `Set<T>`, `[K: V]`).
     */
    class AsBridgeableValue(override val swiftType: SirType) : BidirectionalBridge {
        override val kotlinType = KotlinType.KotlinObject
        override val cType = CType.Object.nullable

        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val ktType = typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)
                return "(dereferenceAndDisposeBridgeableRef($valueExpression) as $ktType)"
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "if ($valueExpression == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "$valueExpression.__retainedToKotlinPointer()"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "${typeNamer.swiftFqName(swiftType)}.__fromKotlinPointer($valueExpression)"
        }
    }

    /**
     * Bridge for generic `SirClass` instances (`Box<T>`, `AnyRepo<T>`, `_KotlinExistential<any _Shape>`),
     * reconstructed via `Type.__fromKotlinPointer` (`_KotlinCustomPointerBridgeable`).
     */
    class AsGenericObject(override val swiftType: SirNominalType) : BidirectionalBridge {
        override val kotlinType = KotlinType.KotlinObject
        override val cType = CType.Object

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
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "${valueExpression}.__externalRCRef()"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "${typeNamer.swiftFqName(swiftType)}.__fromKotlinPointer($valueExpression)"
        }
    }

    /**
     * Bridge for Swift 5.7+ parameterized PAT existentials (`any Repo<Int32>`, `any Repo<T>`).
     * Prefers exact `KotlinBase.__createClassWrapper` conformer identity, falling back to `AnyRepo<Args>`.
     */
    class AsPatExistential(
        override val swiftType: SirExistentialType,
        val anyWrapperType: SirNominalType,
    ) : BidirectionalBridge {
        override val kotlinType = KotlinType.KotlinObject
        override val cType = CType.Object

        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val fqName = typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)
                return "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression) as $fqName"
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "${valueExpression}.__externalRCRef()"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val swiftFqName = typeNamer.swiftFqName(swiftType)
                val wrapperFqName = typeNamer.swiftFqName(anyWrapperType)
                return "{ let _ptr = $valueExpression; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? ($swiftFqName) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return $wrapperFqName.__createTypedWrapper(externalRCRef: _ptr) }()"
            }
        }
    }

    /**
     * `+1` retained variant of [AsPatExistential] used when adapting generic closure slots.
     */
    class AsRetainedPatExistential(val patBridge: AsPatExistential) : BidirectionalBridge {
        override val swiftType: SirExistentialType get() = patBridge.swiftType
        override val kotlinType = KotlinType.KotlinObject
        override val cType = CType.Object.nullable

        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val fqName = typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)
                return "(dereferenceAndDisposeBridgeableRef($valueExpression) as $fqName)"
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "if ($valueExpression == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "KotlinRuntimeSupport._cloneBridgeableRef(($valueExpression).__externalRCRef())"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                patBridge.inSwiftSources.kotlinToSwift(typeNamer, "$valueExpression!")
        }
    }

    /**
     * `+1` retained variant of [AsTyped.Flow] used when adapting generic closure slots (`KotlinTypedFlow<R>`).
     */
    class AsRetainedTypedFlow(val flowBridge: AsTyped.Flow) : BidirectionalBridge {
        override val swiftType: SirType get() = flowBridge.swiftType
        override val kotlinType = KotlinType.KotlinObject
        override val cType = CType.Object.nullable

        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val fqName = typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)
                return "(dereferenceAndDisposeBridgeableRef($valueExpression) as $fqName)"
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "if ($valueExpression == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "KotlinRuntimeSupport._cloneBridgeableRef(($valueExpression).wrapped.__externalRCRef())"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                flowBridge.inSwiftSources.kotlinToSwift(typeNamer, "$valueExpression!")
        }
    }

    /**
     * Nullable-pointer variant of [AsInvariantBlock] used when adapting nested generic closure slots
     * (e.g. curried `(T) -> ((U) -> Pair<T, U>)` or higher-order `((T) -> U) -> R`).
     */
    class AsRetainedBlock(val blockBridge: AsInvariantBlock) : BidirectionalBridge {
        override val swiftType: SirFunctionalType get() = blockBridge.swiftType
        override val kotlinType = KotlinType.KotlinObject
        override val cType = CType.Object.nullable

        override val inKotlinSources: ValueConversion
            get() = blockBridge.inKotlinSources

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val converted = blockBridge.inSwiftSources.swiftToKotlin(typeNamer, "_innerBlock")
                return "{ let _innerBlock = $valueExpression; return $converted }()"
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val converted = blockBridge.inSwiftSources.kotlinToSwift(typeNamer, "_rawBlockPtr")
                return "{ let _rawBlockPtr = ($valueExpression)!; return $converted }()"
            }
        }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer, position: SirTypeVariance): List<SirBridge> =
            blockBridge.helperBridges(typeNamer, position)
    }

    /**
     * Raw nullable pointer (`void * _Nullable` / `UnsafeMutableRawPointer?`) used as C-level transport
     * for erased generic closure parameters and returns.
     */
    class AsRawNullableBridgePtr(override val swiftType: SirType) : BidirectionalBridge {
        override val kotlinType = KotlinType.KotlinObject
        override val cType = CType.Object.nullable
        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                valueExpression

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "($valueExpression ?: kotlin.native.internal.NativePtr.NULL)"
        }
        override val inSwiftSources = IdentityValueConversion
    }

    /**
     * Bridge between swift errors and kotlin throwables
     *
     * Unlike a borrowing [AsObject] parameter, the CONSUMER owns and disposes the `+1` ref the producer created, in both directions
     */
    class AsError(override val swiftType: SirType = SirSwiftModule.error.nominalType()) : BidirectionalBridge {
        override val kotlinType = KotlinType.KotlinObject
        override val cType = CType.Object

        override val inKotlinSources = object : ValueConversion {
            // reverse: Kotlin CONSUMES a Swift-produced Throwable RCRef (owns the +1 -> deref + release + dispose)
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "throwableFromReverseBridge($valueExpression)"

            // forward: Kotlin PRODUCES the ref from the thrown Throwable
            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : ValueConversion {
            // reverse: Swift PRODUCES the ref from the caught Error
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "KotlinRuntimeSupport.kotlinThrowableRCRef(for: $valueExpression)"

            // forward: Swift CONSUMES the ref -> concrete/transparent Error (adopts+disposes via __createClassWrapper)
            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "KotlinRuntimeSupport.swiftError(fromKotlinThrowable: ${typeNamer.swiftFqName(SirNominalType(KotlinRuntimeModule.kotlinBase))}.__createClassWrapper(externalRCRef: $valueExpression))"
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
     * 4. Finally, the Swift function calls `__createProtocolWrapper(externalRCRef: ptr, conformsTo: nil)` to reconstruct the Swift value.
     */
    object AsAnyBridgeable : BidirectionalBridge {
        override val swiftType: SirType = KotlinRuntimeSupportModule.kotlinBridgeableType
        override val kotlinType = KotlinType.KotlinObject
        override val cType = CType.Object
        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                "(dereferenceAndDisposeBridgeableRef($valueExpression) as kotlin.Any)"

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
                return "($valueExpression).__retainedToKotlinPointer()!"
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
     * 4. Finally, the Swift function calls `__createProtocolWrapper(externalRCRef: ptr, conformsTo: SwiftProtocolName.Type.self)` to reconstruct the Swift value.
     */
    class AsExistential(override val swiftType: SirExistentialType) : BidirectionalBridge {
        override val kotlinType = KotlinType.KotlinObject
        override val cType = CType.Object

        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val fqName = typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)
                return "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression) as $fqName"
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = "${valueExpression}.__externalRCRef()"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val kotlinBase = typeNamer.swiftFqName(SirNominalType(KotlinRuntimeModule.kotlinBase))
                val swiftFqName = typeNamer.swiftFqName(swiftType)
                val swiftMetaFqName = typeNamer.swiftFqName(SirType.Metatype(swiftType))
                return "${kotlinBase}.__createProtocolWrapper(externalRCRef: $valueExpression, conformsTo: $swiftMetaFqName.self) as! $swiftFqName"
            }
        }
    }

    sealed class AsTyped : BidirectionalBridge, ValueConversion {

        abstract val origin: SirType.Origin.ReifiedType
        abstract val untypedBridge: BidirectionalBridge

        final override val swiftType: SirType get() = origin.reifiedType
        final override val kotlinType get() = untypedBridge.kotlinType
        final override val cType get() = untypedBridge.cType

        final override val inKotlinSources get() = untypedBridge.inKotlinSources
        final override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val valueExpression = this@AsTyped.swiftToKotlin(typeNamer, valueExpression)
                return untypedBridge.inSwiftSources.swiftToKotlin(typeNamer, valueExpression)
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val valueExpression = untypedBridge.inSwiftSources.kotlinToSwift(typeNamer, valueExpression)
                return this@AsTyped.kotlinToSwift(typeNamer, valueExpression)
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
        class Flow(
            override val origin: SirType.Origin.ReifiedType.Flow,
            override val untypedBridge: BidirectionalBridge,
        ) : AsTyped() {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) = "${valueExpression}.wrapped"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val structFqName = typeNamer.swiftFqName(origin.structType)
                val elementType = typeNamer.swiftFqName(SirType.Metatype(origin.elementType.nonOptional()))
                return "$structFqName.create($valueExpression, $elementType.self)"
            }
        }
    }

    class AsOpaqueObject(override val swiftType: SirType, override val kotlinType: KotlinType, override val cType: CType) : BidirectionalBridge {
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
        override val swiftType: SirType,
        override val cType: CType,
    ) : BidirectionalBridge {
        override val kotlinType = KotlinType.ObjCObjectUnretained
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

    class AsObjCProtocol(
        swiftType: SirExistentialType,
    ) : AsObjCBridged(swiftType, CType.id) {
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

    open class AsNSArray(swiftType: SirNominalType, private val elementBridge: BidirectionalBridge) : AsNSCollection(swiftType, CType.NSArray(elementBridge.cType)) {
        override val inSwiftSources = object : InSwiftSources() {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                return valueExpression.mapSwift { elementBridge.inSwiftSources.swiftToKotlin(typeNamer, it) }
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                if (elementBridge !is AsBlockPointerInCollection) {
                    return super.kotlinToSwift(typeNamer, valueExpression)
                }
                val elementConversion = elementBridge.inSwiftSources.kotlinToSwift(typeNamer, "__element as! Swift.Int")
                return "($valueExpression as! [Any]).map { __element in $elementConversion }"
            }
        }

        override val inKotlinSources: ValueConversion
            get() = when {
                elementBridge is AsBlockPointerInCollection -> object : ValueConversion {
                    context(session: SirSession)
                    override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                        val elementConversion = elementBridge.inKotlinSources.swiftToKotlin(typeNamer, "__element")
                        return "interpretObjCPointer<kotlin.collections.List<kotlin.Any>>($valueExpression).map { __element -> " +
                                "$elementConversion }"
                    }

                    context(session: SirSession)
                    override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                        val elementConversion = elementBridge.inKotlinSources.kotlinToSwift(typeNamer, "__element")
                        return "$valueExpression.map { __element -> $elementConversion }.objcPtr()"
                    }
                }
                elementBridge is AsNSNumber && elementBridge.swiftType.isChar -> object : ValueConversion {
                    context(session: SirSession)
                    override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                        "interpretObjCPointer<kotlin.collections.List<kotlin.Int>>($valueExpression).map { it.toChar() }"

                    context(session: SirSession)
                    override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                        "$valueExpression.map { it.code }.objcPtr()"
                }
                else -> super.inKotlinSources
            }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer, position: SirTypeVariance): List<SirBridge> {
            return super.helperBridges(typeNamer, position) + elementBridge.helperBridges(typeNamer, position)
        }
    }

    class AsNSArrayForVariadic(swiftType: SirNominalType, elementBridge: BidirectionalBridge) : AsNSArray(swiftType, elementBridge) {
        private val elementType get() = (swiftType as SirNominalType).typeArguments.single()

        private fun arrayKind(typeNamer: SirTypeNamer): String? = typeNamer.kotlinPrimitiveFqNameIfAny(elementType)

        context(session: SirSession)
        override fun kotlinReverseParameterTypeFqName(typeNamer: SirTypeNamer): String =
            arrayKind(typeNamer)?.let { "kotlin.${it}Array" }
                ?: "kotlin.Array<out ${typeNamer.kotlinFqName(elementType, SirTypeNamer.KotlinNameType.PARAMETRIZED)}>"

        override val inKotlinSources: ValueConversion = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                if (elementType.isChar) {
                    "interpretObjCPointer<kotlin.collections.List<kotlin.Int>>($valueExpression).let { _l -> kotlin.CharArray(_l.size) { _l[it].toChar() } }"
                } else {
                    "interpretObjCPointer<${
                        typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)
                    }>($valueExpression).to${arrayKind(typeNamer) ?: "Typed"}Array()"
                }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                if (elementType.isChar) {
                    "$valueExpression.map { it.code }.objcPtr()"
                } else {
                    "$valueExpression.toList().objcPtr()"
                }
        }
    }

    class AsNSSet(swiftType: SirNominalType, private val elementBridge: BidirectionalBridge) : AsNSCollection(swiftType, CType.NSSet(elementBridge.cType)) {
        override val inSwiftSources = object : InSwiftSources() {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val transformedElements = valueExpression.mapSwift { elementBridge.inSwiftSources.swiftToKotlin(typeNamer, it) }
                return if (transformedElements == valueExpression) valueExpression else "Set($transformedElements)"
            }
        }

        override val inKotlinSources: ValueConversion
            get() = if (elementBridge is AsNSNumber && elementBridge.swiftType.isChar) {
                object : ValueConversion {
                    context(session: SirSession)
                    override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                        "interpretObjCPointer<kotlin.collections.Set<kotlin.Int>>($valueExpression).mapTo(kotlin.collections.LinkedHashSet()) { it.toChar() }"

                    context(session: SirSession)
                    override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                        "$valueExpression.mapTo(kotlin.collections.LinkedHashSet()) { it.code }.objcPtr()"
                }
            } else {
                super.inKotlinSources
            }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer, position: SirTypeVariance): List<SirBridge> {
            return super.helperBridges(typeNamer, position) + elementBridge.helperBridges(typeNamer, position)
        }
    }

    class AsNSDictionary(swiftType: SirNominalType, private val keyBridge: BidirectionalBridge, private val valueBridge: BidirectionalBridge) :
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

        override val inKotlinSources: ValueConversion
            get() {
                val kIsChar = keyBridge is AsNSNumber && keyBridge.swiftType.isChar
                val vIsChar = valueBridge is AsNSNumber && valueBridge.swiftType.isChar
                return if (kIsChar || vIsChar) {
                    object : ValueConversion {
                        context(session: SirSession)
                        override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                            val kType = if (kIsChar) "kotlin.Int" else typeNamer.kotlinFqName(
                                (swiftType as SirNominalType).typeArguments[0],
                                SirTypeNamer.KotlinNameType.PARAMETRIZED,
                            )
                            val vType = if (vIsChar) "kotlin.Int" else typeNamer.kotlinFqName(
                                (swiftType as SirNominalType).typeArguments[1],
                                SirTypeNamer.KotlinNameType.PARAMETRIZED,
                            )
                            val kConv = if (kIsChar) "_k.toChar()" else "_k"
                            val vConv = if (vIsChar) "_v.toChar()" else "_v"
                            return "interpretObjCPointer<kotlin.collections.Map<$kType, $vType>>($valueExpression).entries.associate { (_k, _v) -> $kConv to $vConv }"
                        }

                        context(session: SirSession)
                        override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                            val kConv = if (kIsChar) "_k.code" else "_k"
                            val vConv = if (vIsChar) "_v.code" else "_v"
                            return "$valueExpression.entries.associate { (_k, _v) -> $kConv to $vConv }.objcPtr()"
                        }
                    }
                } else {
                    super.inKotlinSources
                }
            }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer, position: SirTypeVariance): List<SirBridge> {
            return super.helperBridges(typeNamer, position) + keyBridge.helperBridges(typeNamer, position) +
                    valueBridge.helperBridges(typeNamer, position)
        }
    }

    data object AsNothing : BidirectionalBridge {
        override val swiftType = SirNominalType(SirSwiftModule.never)
        override val kotlinType = KotlinType.Boolean
        override val cType = CType.Bool
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

    data object AsOptionalNothing : BidirectionalBridge {
        override val swiftType = SirNominalType(SirSwiftModule.optional, listOf(SirNominalType(SirSwiftModule.never)))
        override val kotlinType = KotlinType.Boolean
        override val cType = CType.Bool
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

    sealed class AsOptionalWrapper private constructor(
        val wrappedObject: Bridge,
    ) : Bridge {

        class Bidirectional(wrappedObject: BidirectionalBridge) : AsOptionalWrapper(wrappedObject), BidirectionalBridge
        class SwiftToKotlin(wrappedObject: SwiftToKotlinBridge) : AsOptionalWrapper(wrappedObject), SwiftToKotlinBridge
        class KotlinToSwift(wrappedObject: KotlinToSwiftBridge) : AsOptionalWrapper(wrappedObject), KotlinToSwiftBridge

        companion object {
            operator fun invoke(wrappedObject: BidirectionalBridge): Bidirectional = Bidirectional(wrappedObject)
            operator fun invoke(wrappedObject: SwiftToKotlinBridge): SwiftToKotlin = SwiftToKotlin(wrappedObject)
            operator fun invoke(wrappedObject: KotlinToSwiftBridge): KotlinToSwift = KotlinToSwift(wrappedObject)
            operator fun invoke(wrappedObject: Bridge): AsOptionalWrapper = when (wrappedObject) {
                is BidirectionalBridge -> Bidirectional(wrappedObject)
                is SwiftToKotlinBridge -> SwiftToKotlin(wrappedObject)
                is KotlinToSwiftBridge -> KotlinToSwift(wrappedObject)
            }
        }

        override val swiftType = wrappedObject.swiftType.optional()
        val kotlinType = when (wrappedObject) {
            is SwiftToKotlinBridge -> wrappedObject.kotlinType
            is KotlinToSwiftBridge -> wrappedObject.kotlinType
        }
        val cType = when (wrappedObject) {
            is SwiftToKotlinBridge -> wrappedObject.cType
            is KotlinToSwiftBridge -> wrappedObject.cType
        }.nullable

        val inKotlinSources: ValueConversion
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

        val inSwiftSources: ValueConversion = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                if (wrappedObject is AsOptionalWrapper) {
                    return "($valueExpression).flatMap { it in ${wrappedObject.inSwiftSources.swiftToKotlin(typeNamer, "it")} }"
                }
                if (wrappedObject is AsAnyBridgeable) {
                    return "($valueExpression).flatMap { it in it.__retainedToKotlinPointer() }"
                }
                if (wrappedObject is AsPatExistential || wrappedObject is AsRetainedPatExistential || wrappedObject is AsRetainedTypedFlow || wrappedObject is AsRetainedBlock) {
                    return "{ if let it = $valueExpression { return ${wrappedObject.inSwiftSources.swiftToKotlin(typeNamer, "it")} } else { return nil } }()"
                }
                require(
                    wrappedObject is AsObjCBridged || wrappedObject is AsObject ||
                            wrappedObject is AsGenericObject ||
                            wrappedObject is AsExistential || wrappedObject is AsTyped ||
                            wrappedObject is AsInvariantBlock ||
                            wrappedObject is AsError ||
                            wrappedObject is SirCustomTypeTranslatorImpl.RangeBridge
                )
                return valueExpression.mapSwift { wrappedObject.inSwiftSources.swiftToKotlin(typeNamer, it) } +
                        " ?? ${wrappedObject.renderNil()}"
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                return when (wrappedObject) {
                    is AsOptionalWrapper ->
                        "{ switch $valueExpression { case ${wrappedObject.renderNil()}: return .none; default: return .some(${
                            wrappedObject.inSwiftSources.kotlinToSwift(typeNamer, valueExpression)
                        }); } }()"
                    is AsPatExistential, is AsRetainedPatExistential, is AsRetainedTypedFlow, is AsRetainedBlock -> {
                        val innerSwiftType = typeNamer.swiftFqName(wrappedObject.swiftType)
                        val unwrappedConversion = when (wrappedObject) {
                            is AsPatExistential -> wrappedObject.inSwiftSources.kotlinToSwift(typeNamer, "res")
                            is AsRetainedPatExistential -> wrappedObject.patBridge.inSwiftSources.kotlinToSwift(typeNamer, "res")
                            is AsRetainedTypedFlow -> wrappedObject.flowBridge.inSwiftSources.kotlinToSwift(typeNamer, "res")
                            is AsRetainedBlock -> wrappedObject.blockBridge.inSwiftSources.kotlinToSwift(typeNamer, "res")
                        }
                        "{ () -> ($innerSwiftType)? in guard let res = $valueExpression else { return nil }; return $unwrappedConversion }()"
                    }
                    is AsObjCBridged, is AsInvariantBlock ->
                        valueExpression.mapSwift { wrappedObject.inSwiftSources.kotlinToSwift(typeNamer, it) }
                    is AsObject, is AsGenericObject, is AsExistential, is AsAnyBridgeable, is AsTyped, is AsError, is SirCustomTypeTranslatorImpl.RangeBridge ->
                        "{ switch $valueExpression { case ${wrappedObject.renderNil()}: .none; case let res?: ${
                            wrappedObject.inSwiftSources.kotlinToSwift(typeNamer, "res")
                        }; } }()"
                    is AsBridgeableValue,
                    is AsRawNullableBridgePtr,
                    is AsBlockPointerInCollection,
                    is AsIs,
                    is AsOpaqueObject,
                    is AsOutError,
                        -> TODO("not yet supported")

                    is AsNothing -> error("AsOptionalNothing must be used for AsNothing")
                    is AsVoid, is AsNestedOptionalVoid -> error("AsOptionalVoid must be used for AsVoid")
                    is AsOptionalNothing, AsOptionalVoid -> error("there is not optional wrappers for optional")
                }
            }
        }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer, position: SirTypeVariance): List<SirBridge> {
            return wrappedObject.helperBridges(typeNamer, position)
        }
    }

    class AsInvariantBlock private constructor(
        override val swiftType: SirFunctionalType,
        private val contravariantBlock: AsContravariantBlock,
        private val covariantBlock: AsCovariantBlock,
    ) : BidirectionalBridge {
        override val kotlinType = KotlinType.KotlinObject
        override val cType = CType.Object

        companion object {
            context(session: SirSession)
            operator fun invoke(swiftType: SirFunctionalType): AsInvariantBlock {
                return AsInvariantBlock(
                    swiftType,
                    AsContravariantBlock(swiftType),
                    AsCovariantBlock(swiftType),
                )
            }

            context(session: SirSession)
            operator fun invoke(parameters: List<Bridge>, returnType: Bridge): AsInvariantBlock = invoke(
                SirFunctionalType(
                    parameterTypes = parameters.map { it.swiftType.escaping },
                    returnType = returnType.swiftType,
                )
            )
        }

        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String) =
                contravariantBlock.inKotlinSources.swiftToKotlin(typeNamer, valueExpression)

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                covariantBlock.inKotlinSources.kotlinToSwift(typeNamer, valueExpression)
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                contravariantBlock.inSwiftSources.swiftToKotlin(typeNamer, valueExpression)

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                covariantBlock.inSwiftSources.kotlinToSwift(typeNamer, valueExpression)
        }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer, position: SirTypeVariance): List<SirBridge> = when (position) {
            SirTypeVariance.CONTRAVARIANT -> contravariantBlock.helperBridges(typeNamer)
            SirTypeVariance.COVARIANT -> covariantBlock.helperBridges(typeNamer)
            SirTypeVariance.INVARIANT -> error("A block bridge is used in a single direction at a time")
        }

        private class AsContravariantBlock private constructor(
            val swiftType: SirFunctionalType,
            private val erasedSwiftType: SirFunctionalType,
            private val contextParameters: List<KotlinToSwiftBridge>,
            private val parameters: List<KotlinToSwiftBridge>,
            private val returnType: SwiftToKotlinBridge,
            private val rawReturnBridge: BidirectionalBridge?,
            private val asyncParameters: Triple<KotlinToSwiftBridge, KotlinToSwiftBridge, KotlinToSwiftBridge>?,
            private val bridgeProxy: BridgeFunctionProxy?,
        ) {
            companion object {
                context(session: SirSession)
                private fun computeAsyncParameters(
                    returnType: SwiftToKotlinBridge
                ): Triple<KotlinToSwiftBridge, KotlinToSwiftBridge, KotlinToSwiftBridge> = asyncContinuationBridges(returnType)

                context(session: SirSession)
                private fun generateCalleeBridge(erasedSwiftType: SirFunctionalType): BridgeFunctionProxy? = session.generateFunctionBridge(
                    baseBridgeName = session.moduleToTranslate.sirModule().name + "_internal_functional_type_callee_" + erasedSwiftType.bridgeNameSuffix,
                    explicitParameters = listOf(
                        SirParameter(
                            argumentName = "pointerToClosure",
                            type = SirSwiftModule.unsafeMutableRawPointer.nominalType()
                        )
                    ) + erasedSwiftType.contextTypes.mapIndexed { idx, type ->
                        SirParameter(argumentName = "ctx${idx}", type = type)
                    } + erasedSwiftType.parameterTypes.map { SirParameter(type = it) },
                    returnType = erasedSwiftType.returnType,
                    kotlinFqName = FqName(""),
                    kotlinOptIns = erasedSwiftType.allRequiredOptIns,
                    selfParameter = null,
                    contextParameters = emptyList(),
                    extensionReceiverParameter = null,
                    errorParameter = null,
                    isAsync = erasedSwiftType.isAsync
                )

                context(session: SirSession)
                operator fun invoke(
                    swiftType: SirFunctionalType,
                ): AsContravariantBlock {
                    val erasedSwiftType = swiftType.eraseGenericPositions()
                    val parameters = swiftType.parameterTypes.map {
                        if (it.containsTypeParameter()) bridgeGenericClosureSlot(it) else bridgeReturnType(it)
                    }
                    val contextParameters = swiftType.contextTypes.map {
                        if (it.containsTypeParameter()) bridgeGenericClosureSlot(it) else bridgeReturnType(it)
                    }
                    val rawReturnBridge = if (swiftType.returnType.containsTypeParameter()) {
                        bridgeGenericClosureSlot(swiftType.returnType)
                    } else {
                        null
                    }
                    val returnType = rawReturnBridge ?: bridgeParameterType(swiftType.returnType)
                    val erasedReturnType = if (erasedSwiftType.returnType.containsTypeParameter()) {
                        AsRawNullableBridgePtr(rawNullablePointerSirType)
                    } else {
                        bridgeParameterType(erasedSwiftType.returnType)
                    }
                    val asyncParameters = swiftType.isAsync.ifTrue { computeAsyncParameters(erasedReturnType) }

                    return AsContravariantBlock(
                        swiftType,
                        erasedSwiftType,
                        contextParameters,
                        parameters,
                        asyncParameters?.let { AsVoid } ?: returnType,
                        rawReturnBridge,
                        asyncParameters,
                        generateCalleeBridge(erasedSwiftType),
                    )
                }
            }

            val inKotlinSources: SwiftToKotlinValueConversion = object : ValueConversion {
                context(session: SirSession)
                override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                    val argsInClosure = buildList {
                        addAll(contextParameters.mapIndexed { idx, el -> "ctx${idx}" to el })
                        addAll(parameters.mapIndexed { idx, el -> "arg${idx}" to el })
                    }.takeIf { it.isNotEmpty() }
                    val regularConversions = argsInClosure.orEmpty().joinToString(separator = "\n") { [name, bridge] ->
                        "val _$name = ${bridge.inKotlinSources.kotlinToSwift(typeNamer, name)}"
                    }
                    val regularCallArgs = argsInClosure.orEmpty().map { [name, _] -> "_$name" }

                    val body = asyncParameters?.let { async ->
                        val asyncResultExpr = renderKotlinSuspendSwiftCoroutine(typeNamer, async, prelude = regularConversions) { continuation, exception, cancellation ->
                            "val _result = ${invokeClosure(regularCallArgs + listOf(continuation, exception, cancellation))}\n" +
                                    returnType.inKotlinSources.swiftToKotlin(typeNamer, "_result")
                        }
                        if (rawReturnBridge != null) {
                            "val _rawAsync = $asyncResultExpr\n" + rawReturnBridge.inKotlinSources.swiftToKotlin(typeNamer, "(_rawAsync ?: kotlin.native.internal.NativePtr.NULL)")
                        } else {
                            asyncResultExpr
                        }
                    } ?: run {
                        val mappedArgs = regularConversions.takeIf { it.isNotEmpty() }?.let { "$it\n" } ?: ""
                        mappedArgs + "val _result = ${invokeClosure(regularCallArgs)}\n" +
                                returnType.inKotlinSources.swiftToKotlin(typeNamer, "_result")
                    }

                    return """run {
                    |    val closurePtr = $valueExpression;
                    |    val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
                    |    ${asyncParameters?.let { "suspend " } ?: ""}{${argsInClosure.defineArgs(typeNamer)}
                    ${body.prependIndent("|        ")}
                    |    }
                    |}""".trimMargin()
                }

                /** Call of the generated reverse bridge that reaches the boxed Swift closure. */
                private fun invokeClosure(arguments: List<String>): String = bridgeProxy
                    ?.let { "${it.cBridgeName}(${(listOf("closureBox.objcPtr()") + arguments).joinToString()})" }
                    ?: "error(\"Unsupported functional type: ${swiftType.swiftName}\")"

                context(session: SirSession)
                private fun List<Pair<String, KotlinToSwiftBridge>>?.defineArgs(typeNamer: SirTypeNamer): String = this?.let { args ->
                    " ${
                        args.joinToString { [name, bridge] ->
                            "${name}: ${typeNamer.kotlinFqName(bridge.swiftType, SirTypeNamer.KotlinNameType.FQN)}"
                        }
                    } ->"
                } ?: ""
            }

            val inSwiftSources = object : SwiftToKotlinValueConversion {
                context(session: SirSession)
                override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                    if (!swiftType.containsTypeParameter()) {
                        return "Unmanaged.passRetained(($valueExpression as ${swiftType.swiftName}) as AnyObject).toOpaque()"
                    }
                    val allOrigParams = swiftType.contextTypes + swiftType.parameterTypes
                    val allErasedParams = erasedSwiftType.contextTypes + erasedSwiftType.parameterTypes
                    val paramDecls = allErasedParams.mapIndexed { idx, erasedTy ->
                        "_p$idx: ${erasedTy.swiftName}"
                    }.joinToString()
                    val convertedArgs = allOrigParams.mapIndexed { idx, origTy ->
                        if (origTy.containsTypeParameter()) {
                            bridgeGenericClosureSlot(origTy).inSwiftSources.kotlinToSwift(typeNamer, "_p$idx")
                        } else {
                            "_p$idx"
                        }
                    }
                    val ctxCount = swiftType.contextTypes.size
                    val callArgs = buildList {
                        if (ctxCount > 0) add(convertedArgs.take(ctxCount).joinToString(prefix = "(", postfix = ")"))
                        addAll(convertedArgs.drop(ctxCount))
                    }.joinToString()
                    val awaitPrefix = if (swiftType.isAsync) "try await " else ""
                    val callOrig = "$awaitPrefix($valueExpression)($callArgs)"
                    val closureBody = if (rawReturnBridge != null) {
                        "let _res: ${swiftType.returnType.swiftName} = $callOrig; return ${rawReturnBridge.inSwiftSources.swiftToKotlin(typeNamer, "_res")}"
                    } else {
                        "return $callOrig"
                    }
                    val asyncThrows = if (swiftType.isAsync) " async throws" else ""
                    val header = if (allErasedParams.isEmpty() && !swiftType.isAsync) "" else " ($paramDecls)$asyncThrows -> ${erasedSwiftType.returnType.swiftName} in"
                    val adaptedClosure = "{$header $closureBody }"
                    return "Unmanaged.passRetained(($adaptedClosure as ${erasedSwiftType.swiftName}) as AnyObject).toOpaque()"
                }
            }

            private fun invokeBoxedClosure(paramExprs: List<String>): String {
                val pointerToClosure = paramExprs.first()
                val arguments = paramExprs.drop(1)
                val contextArgument = arguments.take(contextParameters.size)
                    .takeIf { it.isNotEmpty() }?.joinToString(prefix = "(", postfix = ")")
                val callArgs = (listOfNotNull(contextArgument) + arguments.drop(contextParameters.size)).joinToString()
                val awaitPrefix = if (erasedSwiftType.isAsync) "try await " else ""
                return "$awaitPrefix(Unmanaged<AnyObject>.fromOpaque($pointerToClosure)" +
                        ".takeUnretainedValue() as! ${erasedSwiftType.swiftName})($callArgs)"
            }

            context(sir: SirSession)
            fun helperBridges(typeNamer: SirTypeNamer): List<SirBridge> {
                return buildList {
                    addAll(bridgeProxy?.createReverseInvocationBridges(::invokeBoxedClosure).orEmpty())
                    swiftType.contextTypes.zip(contextParameters).forEach { [origTy, paramBridge] ->
                        if (origTy.containsTypeParameter()) {
                            addAll(paramBridge.helperBridges(typeNamer, SirTypeVariance.COVARIANT))
                        }
                    }
                    swiftType.parameterTypes.zip(parameters).forEach { [origTy, paramBridge] ->
                        if (origTy.containsTypeParameter()) {
                            addAll(paramBridge.helperBridges(typeNamer, SirTypeVariance.COVARIANT))
                        }
                    }
                    rawReturnBridge?.let {
                        addAll(it.helperBridges(typeNamer, SirTypeVariance.CONTRAVARIANT))
                    }
                }.distinct()
            }
        }

        private class AsCovariantBlock(
            val swiftType: SirFunctionalType,
        ) {
            private val erasedSwiftType: SirFunctionalType = swiftType.eraseGenericPositions()
            private var cachedProxy: BridgeFunctionProxy? = null
            private var proxyInitialized: Boolean = false

            context(session: SirSession)
            private fun getBridgeProxy(typeNamer: SirTypeNamer): BridgeFunctionProxy? {
                if (!proxyInitialized) {
                    val genericSuffix = if (swiftType.containsTypeParameter()) {
                        val optPatTag = (swiftType.contextTypes + swiftType.parameterTypes).joinToString("") {
                            when {
                                it is SirOptionalType && it.wrappedType is SirExistentialType -> "optpat_"
                                it is SirOptionalType && it.wrappedType is SirFunctionalType -> "optfn_"
                                else -> ""
                            }
                        } + (if (swiftType.returnType is SirOptionalType && (swiftType.returnType as SirOptionalType).wrappedType is SirFunctionalType) "retoptfn_" else "")
                        "_generic_" + optPatTag + typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)
                            .replace(Regex("[^A-Za-z0-9_]"), "_")
                    } else {
                        ""
                    }
                    cachedProxy = session.generateFunctionBridge(
                        baseBridgeName = session.moduleToTranslate.sirModule().name + "_internal_functional_type_caller_" + erasedSwiftType.bridgeNameSuffix + genericSuffix,
                        explicitParameters = listOf(
                            SirParameter(
                                argumentName = "pointerToBlock",
                                type = SirSwiftModule.unsafeMutableRawPointer.nominalType()
                            )
                        ) + erasedSwiftType.contextTypes.mapIndexed { idx, type ->
                            SirParameter(argumentName = "ctx${idx}", type = type)
                        } + erasedSwiftType.parameterTypes.map { SirParameter(type = it) },
                        returnType = erasedSwiftType.returnType,
                        kotlinFqName = FqName(""),
                        kotlinOptIns = erasedSwiftType.allRequiredOptIns,
                        selfParameter = null,
                        contextParameters = emptyList(),
                        extensionReceiverParameter = null,
                        errorParameter = null,
                        isAsync = erasedSwiftType.isAsync
                    )
                    proxyInitialized = true
                }
                return cachedProxy
            }

            val inKotlinSources: KotlinToSwiftValueConversion
                get() = object : ValueConversion {
                    context(session: SirSession)
                    override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String) =
                        "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
                }

            val inSwiftSources = object : KotlinToSwiftValueConversion {
                context(session: SirSession)
                override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                    val bridgeProxy = getBridgeProxy(typeNamer)
                    val allArgs = bridgeProxy?.argumentsForInvocation()?.applyIf(swiftType.isAsync) { dropLast(3) }
                        ?: List(1 + swiftType.contextTypes.size + swiftType.parameterTypes.size) { "_" }
                    val defineArgs = buildList {
                        if (swiftType.contextType != null) add("context")
                        addAll(allArgs.drop(1 + swiftType.contextTypes.size))
                    }.takeIf { it.isNotEmpty() }?.let { " ${it.joinToString()} in" } ?: ""
                    val closureHolderRef = "${allArgs.first()}.__externalRCRef()"
                    val allOrigParams = swiftType.contextTypes + swiftType.parameterTypes
                    val overrides = buildMap {
                        put(allArgs.first(), closureHolderRef)
                        if (swiftType.containsTypeParameter()) {
                            allOrigParams.forEachIndexed { idx, origTy ->
                                if (origTy.containsTypeParameter()) {
                                    val argName = if (idx < swiftType.contextTypes.size) "ctx$idx" else allArgs[1 + idx]
                                    put(argName, bridgeGenericClosureSlot(origTy).inSwiftSources.swiftToKotlin(typeNamer, argName))
                                }
                            }
                        }
                    }
                    val returnSlotBridge = if (swiftType.returnType.containsTypeParameter()) {
                        bridgeGenericClosureSlot(swiftType.returnType)
                    } else {
                        null
                    }
                    val swiftInvocation = buildList {
                        if (swiftType.contextType != null) {
                            add(List(swiftType.contextTypes.size) { idx -> "ctx$idx" }.joinToString(prefix = "let (", postfix = ") = context"))
                        }
                        if (bridgeProxy != null) {
                            addAll(bridgeProxy.createSwiftInvocation(overrides) { rawRes ->
                                val converted = returnSlotBridge?.inSwiftSources?.kotlinToSwift(typeNamer, rawRes) ?: rawRes
                                "return $converted"
                            })
                        } else {
                            add("fatalError()")
                        }
                    }
                    val kotlinBaseName = typeNamer.swiftFqName(SirNominalType(KotlinRuntimeModule.kotlinBase))
                    val invokeBody = swiftInvocation.joinToString(";")
                    return """{
                    |    let ${allArgs.first()} = $kotlinBaseName(__externalRCRefUnsafe: $valueExpression, options: .asBestFittingWrapper)
                    |    return {$defineArgs $invokeBody }
                    |}()""".trimMargin()
                }
            }

            context(sir: SirSession)
            fun helperBridges(typeNamer: SirTypeNamer): List<SirBridge> {
                val bridgeProxy = getBridgeProxy(typeNamer)
                val allOrigParams = swiftType.contextTypes + swiftType.parameterTypes
                val returnHasGeneric = swiftType.returnType.containsTypeParameter()
                val ownBridges = bridgeProxy?.createSirBridges {
                    val rawActualArgs = argNames.drop(1 + if (extensionReceiverParameter != null) 1 else 0)
                    val adaptedArgs = rawActualArgs.mapIndexed { idx, argExpr ->
                        val origTy = allOrigParams.getOrNull(idx)
                        if (origTy != null && origTy.containsTypeParameter()) {
                            bridgeGenericClosureSlot(origTy).inKotlinSources.swiftToKotlin(typeNamer, argExpr)
                        } else {
                            argExpr
                        }
                    }
                    val invokeCall = buildCall("(${argNames.first()} as ${typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)}).invoke(${adaptedArgs.joinToString()})")
                    if (returnHasGeneric) {
                        val retBridge = bridgeGenericClosureSlot(swiftType.returnType)
                        "$invokeCall.let { _r -> ${retBridge.inKotlinSources.kotlinToSwift(typeNamer, "_r")} }"
                    } else {
                        invokeCall
                    }
                } ?: emptyList()
                if (!swiftType.containsTypeParameter()) return ownBridges
                return buildList {
                    addAll(ownBridges)
                    allOrigParams.forEach { origTy ->
                        if (origTy.containsTypeParameter()) {
                            addAll(bridgeGenericClosureSlot(origTy).helperBridges(typeNamer, SirTypeVariance.CONTRAVARIANT))
                        }
                    }
                    if (returnHasGeneric) {
                        addAll(bridgeGenericClosureSlot(swiftType.returnType).helperBridges(typeNamer, SirTypeVariance.COVARIANT))
                    }
                }.distinct()
            }
        }
    }

    /**
     * Bridge for block types used as elements of ObjC collections (e.g. NSArray).
     *
     * ObjC collections can only hold `id`s, while a function value travels as a plain reference
     * (see [AsInvariantBlock]). The reference is therefore smuggled through as a `Swift.Int` bit pattern,
     * which bridges to NSNumber; `0` encodes `nil` for optional elements.
     */
    class AsBlockPointerInCollection(
        private val block: AsInvariantBlock,
        override val swiftType: SirType = block.swiftType,
    ) : BidirectionalBridge {
        override val kotlinType = KotlinType.ObjCObjectUnretained
        override val cType = CType.id
        private val isOptional = swiftType is SirOptionalType

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val boxedClosure = block.inSwiftSources
                    .swiftToKotlin(typeNamer, if (isOptional) "__unwrapped" else valueExpression)
                val nonNullConversion = "Swift.Int(bitPattern: $boxedClosure)"
                return if (isOptional) "$valueExpression.map { __unwrapped in $nonNullConversion } ?? 0" else nonNullConversion
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val covariantConversion = block.inSwiftSources.kotlinToSwift(
                    typeNamer, "Swift.UnsafeMutableRawPointer(bitPattern: ${if (isOptional) "__v" else valueExpression})!"
                )
                return if (isOptional) "{ let __v = $valueExpression; return __v == 0 ? nil : $covariantConversion }()" else covariantConversion
            }
        }

        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val reference = "kotlin.native.internal.NativePtr.NULL + ($valueExpression as Long)"
                val nonNullConversion = block.inKotlinSources.swiftToKotlin(typeNamer, reference)
                return if (isOptional) "if (($valueExpression as Long) == 0L) null else $nonNullConversion" else nonNullConversion
            }

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val refCreation = block.inKotlinSources.kotlinToSwift(typeNamer, valueExpression)
                val nonNullConversion = "$refCreation.toLong()"
                return if (isOptional) "if ($valueExpression == null) 0L else $nonNullConversion" else nonNullConversion
            }
        }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer, position: SirTypeVariance): List<SirBridge> =
            block.helperBridges(typeNamer, position)
    }

    object AsOutError : BidirectionalBridge {
        override val swiftType: SirType = SirType.never
        override val kotlinType = KotlinType.PointerToKotlinObject
        override val cType = CType.OutObject.nonnulll
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

private fun Bridge.renderNil(): String = if (this is AsObjCBridgedOptional) "NSNull()" else "nil"

context(session: SirSession)
private fun bridgeGenericClosureSlot(type: SirType): BidirectionalBridge {
    if (type is SirFunctionalType) {
        return AsRetainedBlock(AsInvariantBlock(type))
    }
    if (type is SirExistentialType) {
        val b = bridgeType(type, SirTypeVariance.INVARIANT)
        if (b is AsPatExistential) return AsRetainedPatExistential(b)
        if (b is AsTyped.Flow) return AsRetainedTypedFlow(b)
    }
    if (type is SirOptionalType) {
        val wrapped = type.wrappedType
        if (wrapped is SirFunctionalType) {
            return AsOptionalWrapper(AsRetainedBlock(AsInvariantBlock(wrapped)))
        }
        if (wrapped is SirExistentialType) {
            val b = bridgeType(wrapped, SirTypeVariance.INVARIANT)
            if (b is AsPatExistential) return AsOptionalWrapper(AsRetainedPatExistential(b))
            if (b is AsTyped.Flow) return AsOptionalWrapper(AsRetainedTypedFlow(b))
        }
    }
    return AsBridgeableValue(type)
}

internal fun SirType.isPublicInSwift(): Boolean = when (this) {
    is SirTypeParameterType, is SirSelfType -> true
    is SirNominalType -> {
        val decl = typeDeclaration
        val declPublic = decl.visibility == SirVisibility.PUBLIC ||
                decl.parent == SirSwiftModule ||
                decl.parent == KotlinRuntimeModule ||
                decl.parent == KotlinRuntimeSupportModule
        declPublic && (parent?.isPublicInSwift() != false) && typeArguments.all { it.isPublicInSwift() }
    }
    is SirExistentialType -> protocols.all { [proto, args] ->
        (proto.visibility == SirVisibility.PUBLIC || proto.parent == KotlinRuntimeSupportModule) &&
                args.all { it.isPublicInSwift() }
    }
    else -> false
}

internal fun Bridge.isSwiftInlinableSafe(): Boolean = when (this) {
    is AsIs, AsVoid, AsOptionalVoid, AsOptionalNothing -> true
    is AsBridgeableValue -> swiftType.isPublicInSwift()
    is AsObject -> !swiftType.isValueType && swiftType.isPublicInSwift()
    is AsGenericObject -> swiftType.isPublicInSwift()
    is AsPatExistential -> swiftType.isPublicInSwift() && anyWrapperType.isPublicInSwift()
    is AsNSNumber -> swiftType.isPublicInSwift()
    is AsObjCBridged -> swiftType == SirNominalType(SirSwiftModule.string)
    is AsOptionalWrapper -> wrappedObject.isSwiftInlinableSafe()
    else -> false
}

context(session: SirSession)
private val SirType.allRequiredOptIns: List<ClassId>
    get() = collectRequiredOptIns(mutableSetOf())

context(session: SirSession)
private fun SirType.collectRequiredOptIns(visitedParams: MutableSet<SirTypeParameter>): List<ClassId> = when (this) {
    is SirNominalType -> buildList {
        generateSequence(this@collectRequiredOptIns) { it.parent }.forEach { type ->
            addAll(type.typeArguments.flatMap { it.collectRequiredOptIns(visitedParams) })
            addAll(type.typeDeclaration.allRequiredOptIns)
        }
    }
    is SirExistentialType -> buildList {
        protocols.forEach { [protocol, typeArguments] ->
            addAll(typeArguments.flatMap { it.collectRequiredOptIns(visitedParams) })
            addAll(protocol.allRequiredOptIns)
        }
    }
    is SirFunctionalType -> buildList {
        addAll(contextTypes.flatMap { it.collectRequiredOptIns(visitedParams) })
        addAll(parameterTypes.flatMap { it.collectRequiredOptIns(visitedParams) })
        addAll(errorType.collectRequiredOptIns(visitedParams))
        addAll(returnType.collectRequiredOptIns(visitedParams))
    }
    is SirTupleType -> types.flatMap { it.second.collectRequiredOptIns(visitedParams) }
    is SirType.Metatype -> type.collectRequiredOptIns(visitedParams)
    is SirTypeParameterType -> if (visitedParams.add(parameter)) {
        parameter.bounds.flatMap { it.collectRequiredOptIns(visitedParams) }
    } else {
        emptyList()
    }
    is SirSelfType -> emptyList()
    is SirErrorType, SirUnsupportedType -> emptyList()
}

context(session: SirSession)
private val SirDeclaration.allRequiredOptIns: List<ClassId>
    get() = session.withSessions { kaSymbolOrNull<KaDeclarationSymbol>()?.allRequiredOptIns.orEmpty() }

/**
 * Computes the Kotlin IR erased upper bound (`IrTypeParameter.erasedUpperBound` + `IrType.isNullable`)
 * for reverse-bridge value parameters whose Kotlin type parameter has multiple upper bounds
 * (directly or transitively), matching `@BindReverseBridgeToMethod` overload resolution in `konanc`.
 */
context(session: SirSession)
private fun kotlinMultiBoundReverseParameterTypeFqNameOrNull(sirType: SirType): String? {
    val typeParam: SirTypeParameter
    val markedNullable: Boolean
    when (sirType) {
        is SirTypeParameterType -> {
            typeParam = sirType.parameter
            markedNullable = false
        }
        is SirNominalType -> if (sirType.typeDeclaration == SirSwiftModule.optional) {
            val inner = sirType.typeArguments.singleOrNull() as? SirTypeParameterType ?: return null
            typeParam = inner.parameter
            markedNullable = true
        } else {
            return null
        }
        else -> return null
    }
    val ktParam = (typeParam.origin as? KotlinSource)?.symbol as? KaTypeParameterSymbol ?: return null
    return session.withSessions {
        if (!ktParam.hasTransitiveMultiBounds()) return@withSessions null
        val erasedClass = ktParam.resolveErasedUpperBoundClass() ?: return@withSessions null
        val fqName = erasedClass.classId?.asFqNameString() ?: return@withSessions null
        val starArgs = erasedClass.typeParameters.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "<", postfix = ">") { "*" }
            .orEmpty()
        val isNullable = markedNullable || ktParam.upperBounds.any { it.isIrNullableUpperBound() }
        if (isNullable) "$fqName$starArgs?" else "$fqName$starArgs"
    }
}

context(ka: KaSession)
private fun KaTypeParameterSymbol.hasTransitiveMultiBounds(
    visited: MutableSet<KaTypeParameterSymbol> = mutableSetOf(),
): Boolean = with(ka) {
    if (!visited.add(this@hasTransitiveMultiBounds)) return false
    if (upperBounds.size > 1) return true
    val singleExpanded = upperBounds.singleOrNull()?.fullyExpandedType
    return singleExpanded is KaTypeParameterType && singleExpanded.symbol.hasTransitiveMultiBounds(visited)
}

context(ka: KaSession)
private fun KaTypeParameterSymbol.resolveErasedUpperBoundClass(
    visited: MutableSet<KaTypeParameterSymbol> = mutableSetOf(),
): KaClassLikeSymbol? = with(ka) {
    if (!visited.add(this@resolveErasedUpperBoundClass)) return null
    for (bound in upperBounds) {
        val classSymbol = (bound.fullyExpandedType as? KaClassType)?.symbol as? KaClassSymbol ?: continue
        if (classSymbol.classKind != KaClassKind.INTERFACE && classSymbol.classKind != KaClassKind.ANNOTATION_CLASS) {
            return classSymbol
        }
    }
    val firstExpanded = upperBounds.firstOrNull()?.fullyExpandedType ?: return null
    return when (firstExpanded) {
        is KaClassType -> firstExpanded.symbol
        is KaTypeParameterType -> firstExpanded.symbol.resolveErasedUpperBoundClass(visited)
        else -> null
    }
}

context(ka: KaSession)
private fun KaType.isIrNullableUpperBound(
    visited: MutableSet<KaTypeParameterSymbol> = mutableSetOf(),
): Boolean = with(ka) {
    val expanded = this@isIrNullableUpperBound.fullyExpandedType
    if (expanded.isMarkedNullable) return true
    return expanded is KaTypeParameterType &&
            visited.add(expanded.symbol) &&
            expanded.symbol.upperBounds.any { it.isIrNullableUpperBound(visited) }
}
