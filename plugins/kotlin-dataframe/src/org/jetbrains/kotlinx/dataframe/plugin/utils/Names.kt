/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.utils

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.annotations.ColumnName
import org.jetbrains.kotlinx.dataframe.annotations.Order
import org.jetbrains.kotlinx.dataframe.annotations.ScopeProperty
import kotlin.reflect.KClass

object Names {
    val DF_CLASS_ID: ClassId
        get() = ClassId.topLevel(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe", "DataFrame")))
    val GROUP_BY_CLASS_ID: ClassId
        get() = ClassId.topLevel(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe", "api",  "GroupBy")))

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

    val COLUMNS_SCOPE_CLASS_ID: ClassId
        get() = ClassId(
            FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")),
            Name.identifier("ColumnsScope")
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
    val COLUMN_NAME_ANNOTATION = ClassId(annotationsPackage, Name.identifier(ColumnName::class.simpleName!!))
    val COLUMN_NAME_ARGUMENT = Name.identifier(ColumnName::name.name)

    val DATA_SCHEMA_CLASS_ID = ClassId(annotationsPackage, Name.identifier("DataSchema"))
    val LIST = ClassId(FqName("kotlin.collections"), Name.identifier("List"))
    val DURATION_CLASS_ID = kotlin.time.Duration::class.classId()

    val LOCAL_DATE_CLASS_ID = ClassId(FqName("kotlinx.datetime"), Name.identifier("LocalDate"))
    val LOCAL_DATE_TIME_CLASS_ID = ClassId(FqName("kotlinx.datetime"), Name.identifier("LocalDateTime"))
    val INSTANT_CLASS_ID = ClassId(FqName("kotlinx.datetime"), Name.identifier("Instant"))

    val PAIR = ClassId(FqName("kotlin"), Name.identifier("Pair"))
    val PAIR_CONSTRUCTOR = CallableId(FqName("kotlin"), FqName("Pair"), Name.identifier("Pair"))
    val TO = CallableId(FqName("kotlin"), Name.identifier("to"))
    val TRIM_MARGIN = CallableId(StandardNames.TEXT_PACKAGE_FQ_NAME, Name.identifier("trimMargin"))
    val TRIM_INDENT = CallableId(StandardNames.TEXT_PACKAGE_FQ_NAME, Name.identifier("trimIndent"))
}

private fun KClass<*>.classId(): ClassId {
    val fqName = this.qualifiedName ?: throw IllegalStateException("KClass does not have a qualified name")
    val packageFqName = fqName.substringBeforeLast(".", missingDelimiterValue = "")
    val className = fqName.substringAfterLast(".")
    return ClassId(FqName(packageFqName), Name.identifier(className))
}

fun ConeKotlinType.isDataFrame(session: FirSession) =
    isSubtypeOf(ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(Names.DF_CLASS_ID), arrayOf(ConeStarProjection), isNullable = false), session)

fun ConeKotlinType.isGroupBy(session: FirSession) = fullyExpandedClassId(session) == Names.GROUP_BY_CLASS_ID

fun ConeKotlinType.isDataRow(session: FirSession) = fullyExpandedClassId(session) == Names.DATA_ROW_CLASS_ID
