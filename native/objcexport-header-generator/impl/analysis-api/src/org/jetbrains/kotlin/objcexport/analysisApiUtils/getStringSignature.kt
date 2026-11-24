/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.types.*

/**
 * Returns function signature.
 * It's a bare minimum implementation of [org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorBasedKotlinManglerImpl.signatureString]
 * to match sorting order of K1 implementation
 * ```kotlin
 * fun foo(){} // foo(){}
 * fun foo(a: Int) = Unit // foo(kotlin.Int){}
 * ```
 *
 * See K1 implementation
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorKt.makeMethodsOrderStable]
 */
@OptIn(KaExperimentalApi::class)
context(session: KaSession)
internal fun getStringSignature(symbol: KaFunctionSymbol): String {
    return buildString {
        append(symbol.name)

        if (symbol.isExtension) {
            val receiverClassId = symbol.receiverType?.symbol?.classId
            append("@" + receiverClassId?.packageFqName?.asString() + "." + receiverClassId?.shortClassName)
        }

        /** Translate parameters */
        append('(')
        symbol.valueParameters.joinTo(this, separator = ";", transform = { parameter ->
            val type = parameter.returnType
            if (type is KaTypeParameterType) {
                "0:${symbol.typeParameters.indexOf(type.symbol)}"
            } else {
                if (parameter.isVararg) {
                    if (type.classId in KaStandardTypeClassIds.PRIMITIVES) {
                        toPrimitiveArraySignature(type)
                    } else {
                        toNonPrimitiveArraySignature(type)
                    } + "..."
                } else {
                    type.toString()
                }.replace('/', '.').replace(" ", "")
            }
        })
        append(')')

        /** Translate body */
        append('{')
        append(
            symbol.typeParameters.indices.joinToString(separator = ";", transform = { i ->
                val upperBounds = symbol.typeParameters[i].upperBounds
                if (upperBounds.isEmpty()) {
                    "$i§<${Any::class.qualifiedName}?>"
                } else {
                    "$i§<${upperBounds.joinToString("&")}>"
                }
            })
        )
        append('}')
    }
}

context(session: KaSession)
private fun toPrimitiveArraySignature(type: KaType): String {
    return when {
        type.classId == KaStandardTypeClassIds.INT -> IntArray::class.qualifiedName.toString()
        type.classId == KaStandardTypeClassIds.BOOLEAN -> BooleanArray::class.qualifiedName.toString()
        type.classId == KaStandardTypeClassIds.BYTE -> ByteArray::class.qualifiedName.toString()
        type.classId == KaStandardTypeClassIds.CHAR -> CharArray::class.qualifiedName.toString()
        type.classId == KaStandardTypeClassIds.DOUBLE -> DoubleArray::class.qualifiedName.toString()
        type.classId == KaStandardTypeClassIds.LONG -> LongArray::class.qualifiedName.toString()
        type.classId == KaStandardTypeClassIds.SHORT -> ShortArray::class.qualifiedName.toString()
        else -> ""
    }
}

private fun toNonPrimitiveArraySignature(type: KaType): String {
    return "${Array::class.qualifiedName}<out|$type>"
}
