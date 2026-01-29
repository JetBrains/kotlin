/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl

import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlinx.dataframe.api.insert
import org.jetbrains.kotlinx.dataframe.api.under
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColumnsResolver

/**
 * If column not in the schema but String API refers to it, operation will fail with runtime exception
 * This allows us to sort of "smart cast" the schema
 *  However, only with `Any?` type
 */
context(arguments: Arguments)
fun PluginDataFrameSchema.insertImpliedColumns(selector: List<String>): PluginDataFrameSchema {
    val nullableAny = arguments.session.builtinTypes.nullableAnyType.coneType
    val topLevelColumns = columns().mapToSetOrEmpty { it.name }
    val assumedColumns = selector.filter { it !in topLevelColumns }
        .map { simpleColumnOf(it, nullableAny) }
    return PluginDataFrameSchema(columns() + assumedColumns)
}

fun PluginDataFrameSchema.insertImpliedColumns(selector: ColumnsResolver): PluginDataFrameSchema {
    val df = asDataFrame()
    // groupBy { expr { } } - untitled goes only in `key` columns, shouldn't appear in groups
    // groupBy { "myKey"() } - myKey is implied to exist in original df, goes in both keys and groups
    // just containsColumn is not enough to distinguish these 2 cases, need to know exactly how it was resolved (is it from string api?)
    val columns = selector.resolve(this).filter { it.isImpliedColumn }
    val schema = columns.filterNot { df.containsColumn(it.path) }.fold(df) { acc, column ->
        acc.insert(column.column.asDataColumn()).under(column.path.dropLast())
    }.toPluginDataFrameSchema()
    return schema
}