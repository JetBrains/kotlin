/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class DataColumnValueCounts : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: ResolvedDataColumn by arg()
    val Arguments.dropNA by ignore()
    val Arguments.ascending by ignore()
    val Arguments.sort by ignore()
    val Arguments.resultColumn: String by arg(defaultValue = Present("count"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val value = receiver.col.column
        val countName = if (value.name == resultColumn) resultColumn + "1" else resultColumn
        return PluginDataFrameSchema(
            listOf(receiver.col.column, simpleColumnOf(countName, session.builtinTypes.intType.coneType))
        )
    }
}
