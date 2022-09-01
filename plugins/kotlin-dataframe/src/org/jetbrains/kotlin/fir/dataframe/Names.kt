/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.annotations.Interpretable

object Names {
    val DF_CLASS_ID: ClassId
        get() = ClassId.topLevel(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe", "DataFrame")))
    val COLUM_GROUP_CLASS_ID: ClassId
        get() = ClassId(FqName("org.jetbrains.kotlinx.dataframe.columns"), Name.identifier("ColumnGroup"))
    val DATA_COLUMN_CLASS_ID: ClassId
        get() = ClassId(
            FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")),
            Name.identifier("DataColumn")
        )
    val COLUMNS_CONTAINER_CLASS_ID: ClassId
        get() = ClassId(
            FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")),
            Name.identifier("ColumnsContainer")
        )
    val DATA_ROW_CLASS_ID: ClassId
        get() = ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")), Name.identifier("DataRow"))
    val DF_ANNOTATIONS_PACKAGE: Name
        get() = Name.identifier("org.jetbrains.kotlinx.dataframe.annotations")
    val INTERPRETABLE_FQNAME: FqName
        get() = FqName(Interpretable::class.qualifiedName!!)
}