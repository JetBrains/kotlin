/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.utils

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds

fun ConeKotlinType.isDataFrame(session: FirSession) =
    isSubtypeOf(
        Names.DF_CLASS_ID.constructClassLikeType(arrayOf(ConeStarProjection), isMarkedNullable = false),
        session
    )

fun ConeKotlinType.isGroupBy(session: FirSession) = fullyExpandedClassId(session) == Names.GROUP_BY_CLASS_ID

fun ConeKotlinType.isDataRow(session: FirSession) = fullyExpandedClassId(session) == Names.DATA_ROW_CLASS_ID

fun ConeKotlinType.isPair(session: FirSession) =
    fullyExpandedClassId(session) == Names.PAIR

/**
 * Returns `true` only if [this] represents an optionally nullable primitive number,
 * (like `Double?`, or `Int`), or a "mixed Number" type: `Number?` or `Number`.
 *
 * We don't check for "subtype of Number" to prevent `BigInteger` etc. to be included, but since columns with
 * mixed primitives are allowed in statistics, we do include `Number?` and `Number`
 */
fun ConeKotlinType.isPrimitiveOrMixedNumber(session: FirSession): Boolean =
    this.isPrimitiveNumberOrNullableType ||
            this.equalTypes(
                otherType = session.builtinTypes.numberType.coneType,
                session = session,
            ) ||
            this.equalTypes(
                otherType = session.builtinTypes.numberType.coneType.withNullability(true, session.typeContext),
                session = session,
            )

/** Returns `true` if `this` is a type `T` where `T : Comparable<T & Any>?` */
fun ConeKotlinType.isSelfComparable(session: FirSession): Boolean {
    val comparable = StandardClassIds.Comparable.constructClassLikeType(
        typeArguments = arrayOf(this.withNullability(nullable = false, session.typeContext)),
        isMarkedNullable = this.isMarkedNullable,
    )
    return this.isSubtypeOf(comparable, session)
}
