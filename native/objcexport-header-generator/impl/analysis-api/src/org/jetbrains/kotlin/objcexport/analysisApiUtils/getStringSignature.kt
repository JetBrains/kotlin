package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType

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
internal fun KaSession.getStringSignature(symbol: KaFunctionSymbol): String {
    return buildString {
        append(symbol.name)

        /** Translate parameters */
        append('(')
        symbol.valueParameters.joinTo(this, separator = ";", transform = { parameter ->
            val type = parameter.returnType
            if (type is KaTypeParameterType) {
                "0:${symbol.typeParameters.indexOf(type.symbol)}"
            } else {
                if (parameter.isVararg) {
                    if (type.isPrimitive) {
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

private fun KaSession.toPrimitiveArraySignature(type: KaType): String {
    return when {
        type.isIntType -> IntArray::class.qualifiedName.toString()
        type.isBooleanType -> BooleanArray::class.qualifiedName.toString()
        type.isByteType -> ByteArray::class.qualifiedName.toString()
        type.isCharType -> CharArray::class.qualifiedName.toString()
        type.isDoubleType -> DoubleArray::class.qualifiedName.toString()
        type.isLongType -> LongArray::class.qualifiedName.toString()
        type.isShortType -> ShortArray::class.qualifiedName.toString()
        else -> ""
    }
}

private fun toNonPrimitiveArraySignature(type: KaType): String {
    return "${Array::class.qualifiedName}<out|$type>"
}