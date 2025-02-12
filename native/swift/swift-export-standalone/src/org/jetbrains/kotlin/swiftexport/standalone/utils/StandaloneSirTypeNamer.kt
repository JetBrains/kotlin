/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.utils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.bridge.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.swiftName

internal object StandaloneSirTypeNamer : SirTypeNamer {
    override fun swiftFqName(type: SirType): String = type.swiftName

    override fun kotlinFqName(type: SirType): String = when (type) {
        is SirNominalType -> kotlinFqName(type)
        is SirExistentialType -> kotlinFqName(type)
        is SirGenericType -> type.name
        is SirErrorType, is SirFunctionalType, is SirUnsupportedType -> error("Type $type can not be named")
    }

    private fun kotlinFqName(type: SirExistentialType): String = type.protocols.single().let {
        it.kaSymbolOrNull<KaClassLikeSymbol>()!!.classId!!.asFqNameString()
    }

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

            SirSwiftModule.uint -> "UInt"

            SirSwiftModule.void -> "Void"
            SirSwiftModule.never -> "Nothing"

            SirSwiftModule.array -> "kotlin.collections.List<${kotlinFqName(type.typeArguments.first())}>"
            SirSwiftModule.set -> "kotlin.collections.Set<${kotlinFqName(type.typeArguments.first())}>"
            SirSwiftModule.dictionary -> "kotlin.collections.Map<${kotlinFqName(type.typeArguments[0])}, ${kotlinFqName(type.typeArguments[1])}>"

            SirSwiftModule.optional -> kotlinFqName(type.typeArguments.first()) + "?"

            else -> {
                val symbol = declaration.kaSymbolOrNull<KaClassLikeSymbol>() ?: error("$type doesn't have defined KtSymbol")
                val fqName = symbol.classId?.asFqNameString() ?: error("$symbol doesn't have defined ClassId")
                fqName
            }
        }
    }
}