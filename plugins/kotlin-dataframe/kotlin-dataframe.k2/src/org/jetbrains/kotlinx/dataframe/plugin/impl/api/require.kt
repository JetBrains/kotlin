/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.extensions.ColumnType
import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class Require0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.column: SingleColumnApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.insertImpliedColumns(column)
    }
}

class Require1 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.column: SingleColumnApproximation by arg()
    val Arguments.typeArg1: ColumnType by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        // if the column doesn't exist yet, convertAsColumn considers it "implied"
        return receiver.convertAsColumn(column) {
            simpleColumnOf(it.name(), typeArg1.coneType)
        }
    }
}
