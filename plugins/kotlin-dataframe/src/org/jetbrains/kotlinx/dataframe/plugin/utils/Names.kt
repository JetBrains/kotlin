/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.utils

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.annotations.Order
import org.jetbrains.kotlinx.dataframe.annotations.ScopeProperty
import kotlin.reflect.KClass

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
        get() = FqName("org.jetbrains.kotlinx.dataframe.annotations.Interpretable")
    private val annotationsPackage = FqName("org.jetbrains.kotlinx.dataframe.annotations")
    val ORDER_ANNOTATION = ClassId(annotationsPackage, Name.identifier(Order::class.simpleName!!))
    val ORDER_ARGUMENT = Name.identifier(Order::order.name)
    val SCOPE_PROPERTY_ANNOTATION = ClassId(annotationsPackage, Name.identifier(ScopeProperty::class.simpleName!!))

    val DATA_SCHEMA_CLASS_ID = ClassId(annotationsPackage, Name.identifier("DataSchema"))
    val LIST = ClassId(FqName("kotlin.collections"), Name.identifier("List"))
    val DURATION_CLASS_ID = kotlin.time.Duration::class.classId()
    val LOCAL_DATE_CLASS_ID = kotlinx.datetime.LocalDate::class.classId()
    val LOCAL_DATE_TIME_CLASS_ID = kotlinx.datetime.LocalDateTime::class.classId()
    val INSTANT_CLASS_ID = kotlinx.datetime.Instant::class.classId()
}

private fun KClass<*>.classId(): ClassId {
    val fqName = this.qualifiedName ?: throw IllegalStateException("KClass does not have a qualified name")
    val packageFqName = fqName.substringBeforeLast(".", missingDelimiterValue = "")
    val className = fqName.substringAfterLast(".")
    return ClassId(FqName(packageFqName), Name.identifier(className))
}
