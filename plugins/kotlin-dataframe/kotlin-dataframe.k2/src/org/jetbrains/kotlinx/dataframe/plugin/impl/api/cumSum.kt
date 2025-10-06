/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlinx.dataframe.api.single
import org.jetbrains.kotlinx.dataframe.math.cumSumTypeConversion
import org.jetbrains.kotlinx.dataframe.plugin.extensions.Marker
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame

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
    val Arguments.columns: ColumnsResolver
        get() = columnsResolver {
            colsAtAnyDepth().valueCols().cols {
                (it.single() as Marker).type.isSubtypeOf(
                    superType = session.builtinTypes.numberType.coneType.withNullability(true, session.typeContext),
                    session = session,
                )
            }
        }
    val Arguments.skipNA: Boolean by arg(defaultValue = Present(defaultCumSumSkipNA))

    override fun Arguments.interpret(): PluginDataFrameSchema = getSchemaAfterCumSum(receiver, columns)
}

internal fun Arguments.getSchemaAfterCumSum(dataSchema: PluginDataFrameSchema, selectedColumns: ColumnsResolver): PluginDataFrameSchema {
    val selectedCols = selectedColumns.resolve(dataSchema).mapToSetOrEmpty { it.path.path() }
    return dataSchema.map(selectedCols) { _, col ->
        when (col) {
            is SimpleDataColumn -> {
                val oldConeType = col.type.type()
                val oldKType = oldConeType.toKType() ?: return@map col
                val newKType = cumSumTypeConversion(oldKType, true)
                val newConeType = newKType.toConeKotlinType() ?: return@map col
                col.changeType(newConeType.wrap())
            }
            else -> col
        }
    }
}