/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.isMarkedNullable
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.sir.SirErrorType
import org.jetbrains.kotlin.sir.SirExistentialType
import org.jetbrains.kotlin.sir.SirFunctionalType
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirScopeDefiningDeclaration
import org.jetbrains.kotlin.sir.SirSelfType
import org.jetbrains.kotlin.sir.SirTupleType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirTypeParameter
import org.jetbrains.kotlin.sir.SirTypeParameterType
import org.jetbrains.kotlin.sir.SirTypealias
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.KotlinType
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.KotlinCoroutineSupportModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.utils.resolveUpperBound
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.substituteTypeParameters
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
            return kotlinFqNameWithProjections(origin.type) ?: kotlinParametrizedName(origin.type) ?: error("Failed to name Kotlin type: ${origin.type}")
        }
        return when (type) {
            is SirNominalType -> kotlinFqName(type)
            is SirExistentialType -> kotlinFqName(type)
            is SirTypeParameterType -> kotlinFqName(type.parameter)
            is SirSelfType -> "kotlin.Any"
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
            is SirNominalType -> if (type.typeArguments.isEmpty()) {
                type.typeDeclaration.kaSymbolOrNull<KaClassLikeSymbol>()?.parametrisedTypeName()
            } else {
                kotlinFqName(type)
            }
            is SirExistentialType -> if (type.protocols.singleOrNull()?.second.isNullOrEmpty()) {
                type.protocols.singleOrNull()?.first?.kaSymbolOrNull<KaClassLikeSymbol>()?.parametrisedTypeName()
            } else {
                kotlinFqName(type)
            }
            is SirTypeParameterType -> kotlinFqName(type.parameter)
            is SirSelfType -> "kotlin.Any"
            is SirErrorType, is SirFunctionalType, is SirUnsupportedType, is SirTupleType, is SirType.Metatype -> null
        } ?: kotlinFqName(type)
    }

    context(session: KaSession)
    private fun kotlinFqName(typeParameter: SirTypeParameter): String {
        val ktParam = (typeParameter.origin as? KotlinSource)?.symbol as? KaTypeParameterSymbol
        if (ktParam != null) {
            if (ktParam.upperBounds.isEmpty()) return "kotlin.Any?"
            if (ktParam.upperBounds.size > 1) {
                return if (ktParam.upperBounds.any { it.isMarkedNullable }) "kotlin.Any?" else "kotlin.Any"
            }
            val ub = ktParam.resolveUpperBound() ?: return "kotlin.Any?"
            if (ub.containsKtTypeParameter(ktParam)) {
                if (ub is KaClassType) {
                    val fqName = ub.symbol.classId?.asFqNameString()
                    if (fqName != null) {
                        val starArgs = ub.symbol.typeParameters.takeIf { it.isNotEmpty() }
                            ?.joinToString(prefix = "<", postfix = ">") { "*" } ?: ""
                        return if (ub.isMarkedNullable) "$fqName$starArgs?" else "$fqName$starArgs"
                    }
                }
                return if (ub.isMarkedNullable) "kotlin.Any?" else "kotlin.Any"
            }
            if (ub is KaClassType) {
                val base = kotlinParametrizedName(ub) ?: "kotlin.Any"
                return if (ub.isMarkedNullable && !base.endsWith("?")) "$base?" else base
            }
            return if (ub.isMarkedNullable) "kotlin.Any?" else "kotlin.Any"
        }
        val singleBound = typeParameter.bounds.singleOrNull {
            (it as? SirNominalType)?.typeDeclaration != KotlinRuntimeSupportModule.kotlinBridgeable &&
                    (it as? SirNominalType)?.typeDeclaration != SirSwiftModule.hashable
        }
        return singleBound?.let { kotlinParametrizedName(it) } ?: "kotlin.Any?"
    }

    private fun KaType.containsKtTypeParameter(
        target: KaTypeParameterSymbol,
        visited: MutableSet<KaTypeParameterSymbol> = mutableSetOf(),
    ): Boolean = when (this) {
        is KaTypeParameterType -> symbol == target || (visited.add(symbol) && symbol.upperBounds.any { it.containsKtTypeParameter(target, visited) })
        is KaClassType -> typeArguments.any { (it as? KaTypeArgumentWithVariance)?.type?.containsKtTypeParameter(target, visited) == true }
        is KaFunctionType -> contextParameterTypes.any { it.containsKtTypeParameter(target, visited) } ||
                (receiverType?.containsKtTypeParameter(target, visited) == true) ||
                parameterTypes.any { it.containsKtTypeParameter(target, visited) } ||
                returnType.containsKtTypeParameter(target, visited)
        else -> false
    }

    context(session: KaSession)
    private fun renderTypeArgumentForSymbol(symbol: KaClassLikeSymbol, paramIndex: Int, sirArg: SirType): String {
        val declParam = symbol.typeParameters.getOrNull(paramIndex)
        if (sirArg is SirTypeParameterType) {
            val argKtParam = (sirArg.parameter.origin as? KotlinSource)?.symbol as? KaTypeParameterSymbol
            if (argKtParam != null) {
                if (argKtParam.upperBounds.size > 1) return "*"
                val ub = argKtParam.resolveUpperBound()
                if (ub != null && ub.containsKtTypeParameter(argKtParam)) return "*"
                if (declParam != null && declParam.variance == Variance.IN_VARIANCE &&
                    (ub as? KaClassType)?.symbol?.classId == symbol.classId
                ) {
                    return "*"
                }
            }
            if (declParam != null && (declParam.upperBounds.size > 1 || declParam.resolveUpperBound()?.containsKtTypeParameter(declParam) == true)) {
                return "*"
            }
        }
        val unwrappedSirArg = (sirArg as? SirNominalType)
            ?.takeIf { it.typeDeclaration == SirSwiftModule.optional }
            ?.typeArguments
            ?.firstOrNull()
            ?: sirArg
        if ((unwrappedSirArg as? SirNominalType)?.typeDeclaration == KotlinRuntimeSupportModule.kotlinAny &&
            declParam != null &&
            (declParam.upperBounds.size > 1 || declParam.resolveUpperBound()?.containsKtTypeParameter(declParam) == true)
        ) {
            return "*"
        }
        return kotlinParametrizedName(sirArg)
    }

    context(session: KaSession)
    private fun kotlinFqName(type: SirExistentialType): String = type.protocols.single().let { [protocol, typeArguments] ->
        if (protocol == KotlinRuntimeSupportModule.kotlinBridgeable) return@let "kotlin.Any"
        val symbol = protocol.kaSymbolOrNull<KaClassLikeSymbol>()!!
        val fqName = symbol.classId!!.asFqNameString()
        val typeArgs = when {
            symbol.typeParameters.isEmpty() -> null
            typeArguments.isEmpty() -> symbol.typeParameters.map { "*" }
            else -> typeArguments.mapIndexed { idx, arg -> renderTypeArgumentForSymbol(symbol, idx, arg) }
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
            KotlinRuntimeSupportModule.kotlinUnit -> "Unit"
            KotlinRuntimeSupportModule.kotlinAny -> "kotlin.Any"
            KotlinRuntimeSupportModule.kotlinExistential -> kotlinFqName(type.typeArguments.first())
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

            SirSwiftModule.optional -> kotlinFqName(type.typeArguments.first()).let { if (it.endsWith("?")) it else "$it?" }

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
                    else -> type.typeArguments.mapIndexed { idx, arg -> renderTypeArgumentForSymbol(symbol, idx, arg) }
                }?.joinToString(prefix = "<", postfix = ">") ?: ""
                "$fqName$typeArgs"
            } ?: (declaration as? SirTypealias)?.let { ta ->
                val expanded = if (ta.typeParameters.isNotEmpty() && type.typeArguments.size == ta.typeParameters.size) {
                    ta.type.substituteTypeParameters(ta.typeParameters.zip(type.typeArguments).toMap())
                } else {
                    ta.type
                }
                kotlinFqName(expanded)
            } ?: error("Unnameable declaration $declaration")
        }
    }

    context(session: KaSession)
    private fun kotlinFqNameWithProjections(type: KaType): String? {
        val classType = type as? KaClassType ?: return null
        val fqName = classType.symbol.classId?.asFqNameString() ?: return null
        if (classType.symbol.typeParameters.isEmpty()) {
            return if (classType.isMarkedNullable) "$fqName?" else fqName
        }
        if (classType.typeArguments.size != classType.symbol.typeParameters.size) return null
        val args = classType.typeArguments.map { proj ->
            val argType = proj.type ?: return@map "*"
            kotlinFqNameWithProjections(argType)
                ?: (argType as? KaClassType)?.let { kotlinParametrizedName(it) }
                ?: argType.resolveUpperBound()?.let { kotlinFqNameWithProjections(it) }
                ?: "kotlin.Any?"
        }
        val rendered = "$fqName<${args.joinToString()}>"
        return if (classType.isMarkedNullable) "$rendered?" else rendered
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
