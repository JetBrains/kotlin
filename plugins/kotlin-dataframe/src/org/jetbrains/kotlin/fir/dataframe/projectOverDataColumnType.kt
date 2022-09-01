/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toTypeProjection
import org.jetbrains.kotlin.types.Variance

fun FirResolvedTypeRef.projectOverDataColumnType() =
    ConeClassLikeTypeImpl(
        ConeClassLikeLookupTagImpl(Names.DATA_COLUMN_CLASS_ID),
        arrayOf(coneType.toTypeProjection(Variance.INVARIANT)),
        isNullable = false
    )