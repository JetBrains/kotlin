/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.utils

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlinx.dataframe.annotations.ColumnName
import org.jetbrains.kotlinx.dataframe.annotations.Order

object Names {
    private val DATAFRAME_PACKAGE = FqName("org.jetbrains.kotlinx.dataframe")
    private val DATAFRAME_API_PACKAGE = DATAFRAME_PACKAGE.child(Name.identifier("api"))
    private val DATAFRAME_COLUMNS_PACKAGE = DATAFRAME_PACKAGE.child(Name.identifier("columns"))
    private val DATAFRAME_ANNOTATIONS_PACKAGE = DATAFRAME_PACKAGE.child(Name.identifier("annotations"))
    private val DATAFRAME_IO_PACKAGE = DATAFRAME_PACKAGE.child(Name.identifier("io"))
    private val KOTLINX_DATETIME_PACKAGE = FqName("kotlinx.datetime")
    private val KOTLIN_TIME_PACKAGE = FqName("kotlin.time")
    private val JAVA_TIME_TEMPORAL_PACKAGE = FqName("java.time.temporal")

    val DF_CLASS_ID = ClassId(DATAFRAME_PACKAGE, Name.identifier("DataFrame"))
    val GROUP_BY_CLASS_ID = ClassId(DATAFRAME_API_PACKAGE, Name.identifier("GroupBy"))
    val GROUPED_CLASS_ID = ClassId(DATAFRAME_API_PACKAGE, Name.identifier("Grouped"))
    val REDUCED_GROUP_BY_CLASS_ID = ClassId(DATAFRAME_API_PACKAGE, Name.identifier("ReducedGroupBy"))

    val COLUM_GROUP_CLASS_ID = ClassId(DATAFRAME_COLUMNS_PACKAGE, Name.identifier("ColumnGroup"))
    val FRAME_COLUMN_CLASS_ID = ClassId(DATAFRAME_COLUMNS_PACKAGE, Name.identifier("FrameColumn"))
    val DATA_COLUMN_CLASS_ID = ClassId(DATAFRAME_PACKAGE, Name.identifier("DataColumn"))
    val VALUE_COLUMN_CLASS_ID = ClassId(DATAFRAME_COLUMNS_PACKAGE, Name.identifier("ValueColumn"))
    val BASE_COLUMN_CLASS_ID = ClassId(DATAFRAME_COLUMNS_PACKAGE, Name.identifier("BaseColumn"))
    val COLUMNS_CONTAINER_CLASS_ID = ClassId(DATAFRAME_PACKAGE, Name.identifier("ColumnsContainer"))

    val COLUMNS_SCOPE_CLASS_ID = ClassId(DATAFRAME_PACKAGE, Name.identifier("ColumnsScope"))

    val DATA_ROW_CLASS_ID = ClassId(DATAFRAME_PACKAGE, Name.identifier("DataRow"))
    val INTERPRETABLE_FQNAME = DATAFRAME_ANNOTATIONS_PACKAGE.child(Name.identifier("Interpretable"))
    val STRING_API_INTERPRETABLE_ANNOTATION = ClassId(DATAFRAME_ANNOTATIONS_PACKAGE, Name.identifier("StringApiInterpretable"))
    val ORDER_ANNOTATION = ClassId(DATAFRAME_ANNOTATIONS_PACKAGE, Name.identifier("Order"))
    val CONVERTER_ANNOTATION = ClassId(DATAFRAME_ANNOTATIONS_PACKAGE, Name.identifier("Converter"))
    val ORDER_ARGUMENT = Name.identifier(Order::order.name)
    val SCOPE_PROPERTY_ANNOTATION = ClassId(DATAFRAME_ANNOTATIONS_PACKAGE, Name.identifier("ScopeProperty"))
    val COLUMN_NAME_ANNOTATION = ClassId(DATAFRAME_ANNOTATIONS_PACKAGE, Name.identifier("ColumnName"))
    val DISABLE_INTERPRETATION_ANNOTATION = ClassId(DATAFRAME_ANNOTATIONS_PACKAGE, Name.identifier("DisableInterpretation"))
    val COLUMN_NAME_ARGUMENT = Name.identifier(ColumnName::name.name)
    val DATA_SCHEMA_CLASS_ID = ClassId(DATAFRAME_ANNOTATIONS_PACKAGE, Name.identifier("DataSchema"))

    val DURATION_CLASS_ID = ClassId(KOTLIN_TIME_PACKAGE, Name.identifier("Duration"))
    val LOCAL_DATE_CLASS_ID = ClassId(KOTLINX_DATETIME_PACKAGE, Name.identifier("LocalDate"))
    val LOCAL_DATE_TIME_CLASS_ID = ClassId(KOTLINX_DATETIME_PACKAGE, Name.identifier("LocalDateTime"))
    val INSTANT_CLASS_ID = ClassId(KOTLINX_DATETIME_PACKAGE, Name.identifier("Instant"))
    val STDLIB_INSTANT_CLASS_ID = ClassId(KOTLIN_TIME_PACKAGE, Name.identifier("Instant"))
    val DATE_TIME_PERIOD_CLASS_ID = ClassId(KOTLINX_DATETIME_PACKAGE, Name.identifier("DateTimePeriod"))
    val DATE_TIME_UNIT_CLASS_ID = ClassId(KOTLINX_DATETIME_PACKAGE, Name.identifier("DateTimeUnit"))
    val TIME_ZONE_CLASS_ID = ClassId(KOTLINX_DATETIME_PACKAGE, Name.identifier("TimeZone"))
    val TEMPORAL_ACCESSOR_CLASS_ID = ClassId(JAVA_TIME_TEMPORAL_PACKAGE, Name.identifier("TemporalAccessor"))
    val TEMPORAL_AMOUNT_CLASS_ID = ClassId(JAVA_TIME_TEMPORAL_PACKAGE, Name.identifier("TemporalAmount"))

    val LIST = StandardClassIds.List
    val PAIR = ClassId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("Pair"))
    val PAIR_CONSTRUCTOR = CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, FqName("Pair"), Name.identifier("Pair"))
    val TO = CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("to"))
    val TRIM_MARGIN = CallableId(StandardNames.TEXT_PACKAGE_FQ_NAME, Name.identifier("trimMargin"))
    val TRIM_INDENT = CallableId(StandardNames.TEXT_PACKAGE_FQ_NAME, Name.identifier("trimIndent"))

    val DATAFRAME_PROVIDER = ClassId(DATAFRAME_IO_PACKAGE, Name.identifier("DataFrameProvider"))
    val DATA_SCHEMA_SOURCE_CLASS_ID = ClassId(DATAFRAME_ANNOTATIONS_PACKAGE, Name.identifier("DataSchemaSource"))
    val READ = Name.identifier("read")
    val DEFAULT = Name.identifier("default")
    val SCHEMA_KTYPE = Name.identifier("schemaKType")
}
