/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.types

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.symbols.JKClassSymbol
import org.jetbrains.kotlin.nj2k.symbols.JKTypeParameterSymbol
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

interface JKType {
    val nullability: Nullability
}

interface JKWildCardType : JKType

interface JKParametrizedType : JKType {
    val parameters: List<JKType>
}

interface JKStarProjectionType : JKWildCardType {
    override val nullability: Nullability
        get() = Nullability.NotNull
}

object JKNoType : JKType {
    override val nullability: Nullability = Nullability.NotNull
}

data class JKClassType(
    val classReference: JKClassSymbol,
    override val parameters: List<JKType> = emptyList(),
    override val nullability: Nullability = Nullability.Default
) : JKParametrizedType


object JKStarProjectionTypeImpl : JKStarProjectionType

object JKContextType : JKType {
    override val nullability: Nullability
        get() = Nullability.Default
}

data class JKVarianceTypeParameterType(
    val variance: Variance,
    val boundType: JKType
) : JKWildCardType {
    override val nullability: Nullability
        get() = Nullability.Default

    enum class Variance {
        IN, OUT
    }
}

data class JKTypeParameterType(
    val identifier: JKTypeParameterSymbol,
    override val nullability: Nullability = Nullability.Default
) : JKType

data class JKCapturedType(
    val wildcardType: JKWildCardType,
    override val nullability: Nullability = Nullability.Default
) : JKType

class JKJavaPrimitiveType(val jvmPrimitiveType: JvmPrimitiveType) : JKType {
    override val nullability: Nullability
        get() = Nullability.NotNull

    companion object {
        val BOOLEAN = JKJavaPrimitiveType(JvmPrimitiveType.BOOLEAN)
        val CHAR = JKJavaPrimitiveType(JvmPrimitiveType.CHAR)
        val BYTE = JKJavaPrimitiveType(JvmPrimitiveType.BYTE)
        val SHORT = JKJavaPrimitiveType(JvmPrimitiveType.SHORT)
        val INT = JKJavaPrimitiveType(JvmPrimitiveType.INT)
        val FLOAT = JKJavaPrimitiveType(JvmPrimitiveType.FLOAT)
        val LONG = JKJavaPrimitiveType(JvmPrimitiveType.LONG)
        val DOUBLE = JKJavaPrimitiveType(JvmPrimitiveType.DOUBLE)

        val KEYWORD_TO_INSTANCE = listOf(
            BOOLEAN, CHAR, BYTE, SHORT, INT, FLOAT, LONG, DOUBLE
        ).associateBy { it.jvmPrimitiveType.javaKeywordName } + ("void" to JKJavaVoidType)
    }
}

data class JKJavaArrayType(
    val type: JKType,
    override var nullability: Nullability = Nullability.Default
) : JKType

data class JKJavaDisjunctionType(
    val disjunctions: List<JKType>,
    override val nullability: Nullability = Nullability.Default
) : JKType

object JKJavaVoidType : JKType {
    override val nullability: Nullability
        get() = Nullability.NotNull
}
