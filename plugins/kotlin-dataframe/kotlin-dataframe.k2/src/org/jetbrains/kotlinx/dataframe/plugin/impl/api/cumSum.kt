/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlinx.dataframe.api.single
import org.jetbrains.kotlinx.dataframe.math.cumSumTypeConversion
import org.jetbrains.kotlinx.dataframe.plugin.extensions.Marker
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.utils.isPrimitiveOrMixedNumber

internal val defaultCumSumSkipNA: Boolean = true

/**
 * Handling `df.cumSum(skipNA) { cols }`
 */
class DataFrameCumSum : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()
    val Arguments.skipNA: Boolean by arg(defaultValue = Present(defaultCumSumSkipNA))

    override fun Arguments.interpret(): PluginDataFrameSchema = getSchemaAfterCumSum(receiver, columns)
}

/**
 * Handling `df.cumSum(skipNA)`
 */
class DataFrameCumSum0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.skipNA: Boolean by arg(defaultValue = Present(defaultCumSumSkipNA))

    override fun Arguments.interpret(): PluginDataFrameSchema = getSchemaAfterCumSum(receiver, cumSumDefaultColumns)
}

internal val Arguments.cumSumDefaultColumns: ColumnsResolver
    get() = columnsResolver {
        colsAtAnyDepth().valueCols().cols {
            (it.single() as Marker).coneType.isPrimitiveOrMixedNumber(session)
        }
    }

internal fun Arguments.getSchemaAfterCumSum(dataSchema: PluginDataFrameSchema, selectedColumns: ColumnsResolver): PluginDataFrameSchema {
    val selectedCols = selectedColumns.resolve(dataSchema).mapToSetOrEmpty { it.path.path() }
    return dataSchema.map(selectedCols) { _, col ->
        when (col) {
            is SimpleDataColumn -> {
                val oldConeType = col.type.coneType
                val oldKType = oldConeType.asPrimitiveToKTypeOrNull() ?: return@map col
                val newKType = cumSumTypeConversion(oldKType, true)
                val newConeType = newKType.toConeKotlinType() ?: return@map col
                col.changeType(newConeType.wrap())
            }
            else -> col
        }
    }
}