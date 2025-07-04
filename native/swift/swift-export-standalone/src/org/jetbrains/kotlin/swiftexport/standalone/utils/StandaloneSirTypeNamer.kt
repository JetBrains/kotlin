/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.utils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.bridge.SirTypeNamer
import org.jetbrains.kotlin.sir.bridge.SirTypeNamer.KotlinNameType
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.swiftName

internal object StandaloneSirTypeNamer : SirTypeNamer {
    override fun swiftFqName(type: SirType): String = type.swiftName

    override fun kotlinFqName(sirType: SirType, nameType: KotlinNameType): String = when (nameType) {
        KotlinNameType.FQN -> kotlinFqName(sirType)
        KotlinNameType.PARAMETRIZED -> kotlinParametrizedName(sirType)
    }

    private fun kotlinFqName(type: SirType): String = when (type) {
        is SirNominalType -> kotlinFqName(type)
        is SirExistentialType -> kotlinFqName(type)
        is SirErrorType, is SirFunctionalType, is SirUnsupportedType -> error("Type $type can not be named")
    }

    private fun kotlinParametrizedName(type: SirType): String =
        (type as? SirNominalType)?.typeDeclaration?.kaSymbolOrNull<KaClassLikeSymbol>()?.parametrisedTypeName() ?: kotlinFqName(type)

    private fun kotlinFqName(type: SirExistentialType): String = type.protocols.single().let {
        it.kaSymbolOrNull<KaClassLikeSymbol>()!!.classId!!.asFqNameString()
    }

    @OptIn(KaExperimentalApi::class)
    private fun kotlinFqName(type: SirNominalType): String {
        return when (val declaration = type.typeDeclaration) {
            KotlinRuntimeModule.kotlinBase -> "kotlin.Any"
            SirSwiftModule.string -> "kotlin.String"

            SirSwiftModule.bool -> "Boolean"

            SirSwiftModule.int8 -> "Byte"
            SirSwiftModule.int16 -> "Short"
            SirSwiftModule.int32 -> "Int"
            SirSwiftModule.int64 -> "Long"

            SirSwiftModule.uint8 -> "UByte"
            SirSwiftModule.uint16 -> "UShort"
            SirSwiftModule.uint32 -> "UInt"
            SirSwiftModule.uint64 -> "ULong"

            SirSwiftModule.double -> "Double"
            SirSwiftModule.float -> "Float"

            SirSwiftModule.utf16CodeUnit -> "Char"

            SirSwiftModule.unsafeMutableRawPointer -> "kotlin.native.internal.NativePtr"

            SirSwiftModule.void -> "Void"
            SirSwiftModule.never -> "Nothing"

            SirSwiftModule.array -> "kotlin.collections.List<${kotlinParametrizedName(type.typeArguments.first())}>"
            SirSwiftModule.set -> "kotlin.collections.Set<${kotlinParametrizedName(type.typeArguments.first())}>"
            SirSwiftModule.dictionary -> "kotlin.collections.Map<${kotlinParametrizedName(type.typeArguments[0])}, ${kotlinParametrizedName(type.typeArguments[1])}>"

            SirSwiftModule.optional -> kotlinFqName(type.typeArguments.first()) + "?"

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
                    fqname -> classId?.asFqNameString() + "<${typeParameters.joinToString { "*" }}>"
                    else -> it?.symbol?.parametrisedTypeName()
                }
            }
            .map { it ?: "kotlin.Any?" }

        return "$fqname<${typesRendered.joinToString()}>"
    }

}
