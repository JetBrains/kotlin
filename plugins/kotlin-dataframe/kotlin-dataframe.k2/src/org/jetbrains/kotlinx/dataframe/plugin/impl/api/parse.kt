/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.types.isCharOrNullableChar
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isNullableString
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.*

/**
 * `df.parse { ... }`
 */
class Parse : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.options by ignore()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        receiver.convertAsColumn(columns) { it.changeParsableType() }
}

/**
 * `df.parse("a", "b")`
 */
class ParseString : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.options by ignore()
    val Arguments.columns: List<String> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): PluginDataFrameSchema =
        receiver
            .insertImpliedColumns(columns)
            .convertAsColumn(columnsResolver { columns.toColumnSet() }) {
                it.changeParsableType()
            }
}

/**
 * `df.parse()`
 */
class ParseDefault : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.options by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        PluginDataFrameSchema(
            // We simply take the top-level columns.
            // In the library implementation `parse()` calls `colsAtAnyDepth()`
            // but since `changeParsableType()` is recursive anyway, the result is the same
            columns = receiver.columns().map { it.changeParsableType() },
        )
}

private fun SimpleDataColumn.canBeParsed() =
    type.coneType.let {
        it.isString || it.isNullableString || it.isCharOrNullableChar
    }

/**
 * Changes column types:
 * - `String(?)` -> `Any(?)`
 * - `Char(?)` -> `Any(?)`
 *
 * For this column, or for all columns under this column at any depth.
 */
context(context: SessionHolder)
private fun SimpleCol.changeParsableType(): SimpleCol =
    when (this) {
        is SimpleColumnGroup ->
            this.copy(
                columns = columns().map { it.changeParsableType() },
            )
        is SimpleFrameColumn ->
            this.copy(
                columns = columns().map { it.changeParsableType() },
            )
        is SimpleDataColumn if canBeParsed() ->
            this.copy(
                type =
                    when (type.coneType.isMarkedNullable) {
                        true -> context.session.builtinTypes.nullableAnyType
                        false -> context.session.builtinTypes.anyType
                    }.coneType.wrap(),
            )

        is SimpleDataColumn -> this
    }