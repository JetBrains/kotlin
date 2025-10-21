/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.replace
import org.jetbrains.kotlinx.dataframe.api.with
import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class DataFrameUnfold : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.properties by ignore()
    val Arguments.maxDepth: Int by arg(defaultValue = Present(0))
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.asDataFrame().replace { columns }.with {
            val column = it.asSimpleColumn() as? SimpleDataColumn
            if (column != null) {
                if (!column.type.coneType.canBeUnfolded(session)) {
                    it
                } else {
                    SimpleColumnGroup(it.name(), toDataFrame(maxDepth, column.type.coneType, TraverseConfiguration()).columns()).asDataColumn()
                }
            } else {
                it
            }
        }.toPluginDataFrameSchema()
    }
}
