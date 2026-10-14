/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.isMarkedNullable
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.sir.SirErrorType
import org.jetbrains.kotlin.sir.SirExistentialType
import org.jetbrains.kotlin.sir.SirFunctionalType
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirScopeDefiningDeclaration
import org.jetbrains.kotlin.sir.SirTupleType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.source.KotlinType
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.KotlinCoroutineSupportModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.utils.resolveUpperBound
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.swiftName
import org.jetbrains.kotlin.types.Variance

internal object StandaloneSirTypeNamer : SirTypeNamer {
    override fun swiftFqName(type: SirType): String = type.swiftName

    context(session: SirSession)
    override fun kotlinFqName(sirType: SirType, nameType: SirTypeNamer.KotlinNameType): String = session.withSessions {
        when (nameType) {
            SirTypeNamer.KotlinNameType.FQN -> kotlinFqName(sirType)
            SirTypeNamer.KotlinNameType.PARAMETRIZED -> kotlinParametrizedName(sirType)
        }
    }

    override fun kotlinPrimitiveFqNameIfAny(sirType: SirType): String? {
        return (sirType as? SirNominalType)?.typeDeclaration?.primitiveFqNameIfAny()
    }

    private fun SirScopeDefiningDeclaration.primitiveFqNameIfAny(): String? {
        return primitiveFqNameMap[this]
    }

    private val primitiveFqNameMap = hashMapOf<SirScopeDefiningDeclaration, String>(
        SirSwiftModule.bool to "Boolean",

        SirSwiftModule.int8 to "Byte",
        SirSwiftModule.int16 to "Short",
        SirSwiftModule.int32 to "Int",
        SirSwiftModule.int64 to "Long",

        SirSwiftModule.uint8 to "UByte",
        SirSwiftModule.uint16 to "UShort",
        SirSwiftModule.uint32 to "UInt",
        SirSwiftModule.uint64 to "ULong",

        SirSwiftModule.double to "Double",
        SirSwiftModule.float to "Float",

        SirSwiftModule.utf16CodeUnit to "Char",
    )

    context(session: KaSession)
    private fun kotlinFqName(type: SirType): String {
        val origin = type.origin
        if (origin is SirType.Origin.ReifiedType) {
            return kotlinFqName(origin.erasedType)
        } else if (origin is KotlinType) {
            return kotlinParametrizedName(origin.type) ?: error("Failed to name Kotlin type: ${origin.type}")
        }
        return when (type) {
            is SirNominalType -> kotlinFqName(type)
            is SirExistentialType -> kotlinFqName(type)
            is SirFunctionalType -> "${"kotlin.coroutines.Suspend".takeIf { type.isAsync } ?: ""}Function${type.contextTypes.count() + type.parameterTypes.count()}<${(type.contextTypes + type.parameterTypes + type.returnType).joinToString { kotlinFqName(it) }}>"
            is SirErrorType, is SirUnsupportedType, is SirTupleType, is SirType.Metatype ->
                error("Type $type can not be named")
        }
    }

    context(session: KaSession)
    private fun kotlinParametrizedName(type: SirType): String {
        val origin = type.origin
        if (origin is SirType.Origin.ReifiedType) {
            return kotlinParametrizedName(origin.erasedType)
        } else if (origin is KotlinType) {
            return kotlinParametrizedName(origin.type) ?: error("Failed to name Kotlin type: ${origin.type}")
        }
        return when (type) {
            is SirNominalType -> type.typeDeclaration.kaSymbolOrNull<KaClassLikeSymbol>()?.parametrisedTypeName()
            is SirExistentialType -> type.protocols.singleOrNull()?.first?.kaSymbolOrNull<KaClassLikeSymbol>()?.parametrisedTypeName()
            is SirErrorType, is SirFunctionalType, is SirUnsupportedType, is SirTupleType, is SirType.Metatype -> null
        } ?: kotlinFqName(type)
    }

    context(session: KaSession)
    private fun kotlinFqName(type: SirExistentialType): String = type.protocols.single().let { [protocol, typeArguments] ->
        if (protocol == KotlinRuntimeSupportModule.kotlinBridgeable) return@let "kotlin.Any"
        val symbol = protocol.kaSymbolOrNull<KaClassLikeSymbol>()!!
        val fqName = symbol.classId!!.asFqNameString()
        val typeArgs = when {
            symbol.typeParameters.isEmpty() -> null
            typeArguments.isEmpty() -> symbol.typeParameters.map { "*" }
            else -> typeArguments.map { kotlinParametrizedName(it) }
        }?.joinToString(prefix = "<", postfix = ">") ?: ""
        "$fqName$typeArgs"
    }

    context(session: KaSession)
    private fun kotlinFqName(type: SirNominalType): String {
        val declaration = type.typeDeclaration
        declaration.primitiveFqNameIfAny()?.let { return it }
        return when (declaration) {
            KotlinRuntimeModule.kotlinBase -> "kotlin.Any"
            KotlinRuntimeSupportModule.kotlinBridgeable -> "kotlin.Any"
            KotlinCoroutineSupportModule.swiftJob -> "SwiftJob"
            SirSwiftModule.anyHashable -> "kotlin.Any"
            SirSwiftModule.string -> "kotlin.String"

            SirSwiftModule.unsafeMutableRawPointer -> "kotlin.native.internal.NativePtr"

            SirSwiftModule.void -> "Unit"
            SirSwiftModule.never -> "Nothing"
            SirSwiftModule.error -> "kotlin.Throwable"

            SirSwiftModule.array -> "kotlin.collections.List<${kotlinParametrizedName(type.typeArguments.first())}>"
            SirSwiftModule.set -> "kotlin.collections.Set<${kotlinParametrizedName(type.typeArguments.first())}>"
            SirSwiftModule.dictionary -> "kotlin.collections.Map<${kotlinParametrizedName(type.typeArguments[0])}, ${kotlinParametrizedName(type.typeArguments[1])}>"

            SirSwiftModule.optional -> kotlinFqName(type.typeArguments.first()) + "?"

            SirSwiftModule.range -> "kotlin.ranges.OpenEndRange<${kotlinParametrizedName(type.typeArguments.first())}>"
            SirSwiftModule.closedRange -> {
                val firstArgument = type.typeArguments.first()
                when ((firstArgument as? SirNominalType)?.typeDeclaration) {
                    SirSwiftModule.int64 -> "kotlin.ranges.LongRange"
                    SirSwiftModule.int32 -> "kotlin.ranges.IntRange"
                    else -> "kotlin.ranges.ClosedRange<${kotlinParametrizedName(firstArgument)}>"
                }
            }

            else -> declaration.kaSymbolOrNull<KaClassLikeSymbol>()?.let { symbol ->
                val fqName = symbol.classId?.asFqNameString() ?: return@let null
                val typeArgs = when {
                    symbol.typeParameters.isEmpty() -> null
                    type.typeArguments.isEmpty() -> symbol.typeParameters.map { "*" }
                    else -> type.typeArguments.map { kotlinParametrizedName(it) }
                }?.joinToString(prefix = "<", postfix = ">") ?: ""
                "$fqName$typeArgs"
            } ?: error("Unnameable declaration $declaration")
        }
    }

    context(session: KaSession)
    private fun kotlinParametrizedName(type: KaType): String? {
        check(type is KaClassType) { "Can't name non-class Kotlin type: $type" }
        return type.symbol.parametrisedTypeName(type.typeArguments.map { it.type })
    }

    context(session: KaSession)
    private fun KaClassLikeSymbol.parametrisedTypeName(typeArguments: List<KaType?>? = null): String? {
        require(typeArguments == null || typeParameters.size == typeArguments.size) {
            "type argument count must match type parameter count"
        }
        val fqname = classId?.asFqNameString()
            ?: return null
        if (typeParameters.isEmpty())
            return fqname

        val typeArguments = typeArguments ?: typeParameters.map { null }
        val typesRendered = typeParameters.zip(typeArguments) { param, arg ->
            var type = arg?.resolveUpperBound()
            var isUpperBound = type != arg
            // Two cases when we use the upper bound from the parameter:
            // 1. we don't have an argument
            // 2. for in variance parameters that have an (upper bound) argument equal to this type
            if (arg == null || (param.variance == Variance.IN_VARIANCE && type?.symbol?.classId?.asFqNameString() == fqname)) {
                type = param.resolveUpperBound()
                isUpperBound = true
            }
            when {
                type !is KaClassType -> null
                isUpperBound && type.symbol.classId?.asFqNameString() == fqname -> "*"
                else -> type.symbol.parametrisedTypeName(type.typeArguments.map { it.type })?.let {
                    when {
                        it.contains("*") -> "*"
                        type.isMarkedNullable -> "$it?"
                        else -> it
                    }
                }
            } ?: "kotlin.Any?"
        }

        return "$fqname<${typesRendered.joinToString()}>"
    }
}
