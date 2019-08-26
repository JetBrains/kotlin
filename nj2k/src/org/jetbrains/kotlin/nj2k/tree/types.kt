/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.tree

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.symbols.JKClassSymbol
import org.jetbrains.kotlin.nj2k.symbols.JKTypeParameterSymbol
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

interface JKWildCardType : JKType

interface JKVarianceTypeParameterType : JKWildCardType {
    val variance: Variance
    val boundType: JKType
    override val nullability: Nullability
        get() = Nullability.Default

    enum class Variance {
        IN, OUT
    }
}

interface JKTypeParameterType : JKType {
    val identifier: JKTypeParameterSymbol
}

interface JKNoType : JKType

interface JKParametrizedType : JKType {
    val parameters: List<JKType>
}

interface JKClassType : JKParametrizedType {
    val classReference: JKClassSymbol
    override val nullability: Nullability
}

interface JKJavaPrimitiveType : JKType {
    val jvmPrimitiveType: JvmPrimitiveType
    override val nullability: Nullability
        get() = Nullability.NotNull
}

interface JKJavaArrayType : JKType {
    val type: JKType
}

interface JKStarProjectionType : JKWildCardType {
    override val nullability: Nullability
        get() = Nullability.NotNull
}

interface JKJavaDisjunctionType : JKType {
    val disjunctions: List<JKType>
}