/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.utils

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.isSubtypeOf

fun ConeKotlinType.isDataFrame(session: FirSession) =
    isSubtypeOf(
        Names.DF_CLASS_ID.constructClassLikeType(arrayOf(ConeStarProjection), isMarkedNullable = false),
        session
    )

fun ConeKotlinType.isGroupBy(session: FirSession) = fullyExpandedClassId(session) == Names.GROUP_BY_CLASS_ID

fun ConeKotlinType.isDataRow(session: FirSession) = fullyExpandedClassId(session) == Names.DATA_ROW_CLASS_ID