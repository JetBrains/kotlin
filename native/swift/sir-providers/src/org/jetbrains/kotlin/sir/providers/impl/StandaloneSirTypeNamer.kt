/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.sir.SirErrorType
import org.jetbrains.kotlin.sir.SirExistentialType
import org.jetbrains.kotlin.sir.SirFunctionalType
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirScopeDefiningDeclaration
import org.jetbrains.kotlin.sir.SirTupleType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.providers.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.KotlinCoroutineSupportModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.swiftName

internal object StandaloneSirTypeNamer : SirTypeNamer {
    override fun swiftFqName(type: SirType): String = type.swiftName

    override fun kotlinFqName(sirType: SirType, nameType: SirTypeNamer.KotlinNameType): String = when (nameType) {
        SirTypeNamer.KotlinNameType.FQN -> kotlinFqName(sirType)
        SirTypeNamer.KotlinNameType.PARAMETRIZED -> kotlinParametrizedName(sirType)
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

    private fun kotlinFqName(type: SirType): String = when (type) {
        is SirNominalType -> kotlinFqName(type)
        is SirExistentialType -> kotlinFqName(type)
        is SirFunctionalType -> "${"kotlin.coroutines.Suspend".takeIf { type.isAsync } ?: ""}Function${type.parameterTypes.count()}<${(type.parameterTypes + type.returnType).joinToString { kotlinFqName(it) }}>"
        is SirErrorType, is SirUnsupportedType, is SirTupleType ->
            error("Type $type can not be named")
    }

    private fun kotlinParametrizedName(type: SirType): String = when (type) {
        is SirNominalType -> type.typeDeclaration.kaSymbolOrNull<KaClassLikeSymbol>()?.parametrisedTypeName()
        is SirExistentialType -> type.protocols.singleOrNull()?.kaSymbolOrNull<KaClassLikeSymbol>()?.parametrisedTypeName()
        is SirErrorType, is SirFunctionalType, is SirUnsupportedType, is SirTupleType -> null
    } ?: kotlinFqName(type)

    private fun kotlinFqName(type: SirExistentialType): String = type.protocols.single().let {
        if (it == KotlinRuntimeSupportModule.kotlinBridgeable) "kotlin.Any"
        else it.kaSymbolOrNull<KaClassLikeSymbol>()!!.classId!!.asFqNameString()
    }

    @OptIn(KaExperimentalApi::class)
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

            else -> declaration.kaSymbolOrNull<KaClassLikeSymbol>()?.classId?.asFqNameString()
                ?: error("Unnameable declaration $declaration")
        }
    }

    @OptIn(KaExperimentalApi::class)
    private fun KaClassLikeSymbol.parametrisedTypeName(): String? {
        val fqname = classId?.asFqNameString()
            ?: return null
        if (typeParameters.isEmpty())
            return fqname

        val typesRendered = typeParameters.map { it.upperBounds.firstOrNull() }
            .map {
                when (it?.symbol?.classId?.asFqNameString()) {
                    fqname -> "*"
                    else -> it?.symbol?.parametrisedTypeName()
                }
            }
            .map { it ?: "kotlin.Any?" }

        return "$fqname<${typesRendered.joinToString()}>"
    }
}
