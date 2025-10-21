/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.convertAsColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.simpleColumnOf
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

class Implode : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by dataFrame()
    val Arguments.dropNA: Boolean by arg(defaultValue = Present(false))
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.convertAsColumn(columns) { implode(it, dropNA = dropNA) }
    }
}

class ImplodeDefault : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by dataFrame()
    val Arguments.dropNA: Boolean by arg(defaultValue = Present(false))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.convertAsColumn(columnsResolver { all() }) { implode(it, dropNA = dropNA) }
    }
}

fun Arguments.implode(col: SimpleCol, dropNA: Boolean = false): SimpleCol = when (col) {
    is SimpleColumnGroup -> SimpleFrameColumn(col.name(), col.columns())
    is SimpleDataColumn -> {
        val nullable = if (dropNA) false else col.type.coneType.isMarkedNullable
        simpleColumnOf(
            col.name, createListType(col.type.coneType.withNullability(nullable, session.typeContext))
        )
    }
    is SimpleFrameColumn -> simpleColumnOf(
        // For now, we can't propagate the schema like List<DataFrame<SchemaType>> - the column type becomes List<DataFrame<*>>.
        // but it's the same as in the library
        col.name, createListType(Names.DF_CLASS_ID.createConeType(session, arrayOf(ConeStarProjection)))
    )
}