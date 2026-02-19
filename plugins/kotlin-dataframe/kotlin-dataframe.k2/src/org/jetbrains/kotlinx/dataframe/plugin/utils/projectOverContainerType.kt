/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.utils

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.toTypeProjection
import org.jetbrains.kotlin.types.Variance

fun ConeKotlinType.projectOverDataColumnType() =
    Names.DATA_COLUMN_CLASS_ID.constructClassLikeType(
        typeArguments = arrayOf(this.toTypeProjection(Variance.INVARIANT))
    )

fun ConeKotlinType.projectOverDataRowType() =
    Names.DATA_ROW_CLASS_ID.constructClassLikeType(
        typeArguments = arrayOf(this.toTypeProjection(Variance.INVARIANT))
    )