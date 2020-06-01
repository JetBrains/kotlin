/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirSimpleTypeKind.CLASS
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirSimpleTypeKind.TYPE_ALIAS
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.AbbreviatedType
import org.jetbrains.kotlin.types.Variance

sealed class CirType {
    abstract val fqNameWithTypeParameters: String
}

/**
 * All attributes except for [expandedTypeConstructorId] are read from the abbreviation type: [AbbreviatedType.abbreviation].
 * And [expandedTypeConstructorId] is read from the expanded type: [AbbreviatedType.expandedType].
 *
 * This is necessary to properly compare types for type aliases, where abbreviation type represents the type alias itself while
 * expanded type represents right-hand side declaration that should be processed separately.
 *
 * There is no difference between "abbreviation" and "expanded" for types representing classes and type parameters.
 * For details, see [CirTypeFactory].
 *
 * Note: Annotations at simple types are not preserved. After commonization all annotations assigned to types will be lost.
 */
abstract class CirSimpleType : CirType() {
    abstract val kind: CirSimpleTypeKind
    abstract val fqName: FqName
    abstract val arguments: List<CirTypeProjection>
    abstract val isMarkedNullable: Boolean
    abstract val isDefinitelyNotNullType: Boolean
    abstract val expandedTypeConstructorId: CirTypeConstructorId

    inline val isClassOrTypeAlias: Boolean get() = (kind == CLASS || kind == TYPE_ALIAS)
}

enum class CirSimpleTypeKind {
    CLASS,
    TYPE_ALIAS,
    TYPE_PARAMETER;

    companion object {
        fun areCompatible(expect: CirSimpleTypeKind, actual: CirSimpleTypeKind): Boolean =
            expect == actual || (expect == CLASS && actual == TYPE_ALIAS)
    }
}

data class CirTypeConstructorId(val fqName: FqName, val numberOfTypeParameters: Int)

data class CirTypeProjection(val projectionKind: Variance, val isStarProjection: Boolean, val type: CirType)

data class CirFlexibleType(val lowerBound: CirSimpleType, val upperBound: CirSimpleType) : CirType() {
    override val fqNameWithTypeParameters: String
        get() = lowerBound.fqNameWithTypeParameters
}
