/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlinx.dataframe.plugin.findSchemaArgument
import org.jetbrains.kotlinx.dataframe.plugin.getSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Interpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame

class DataFrameAddAll : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.dataFrames: FirVarargArgumentsExpression by arg(lens = Interpreter.Id)

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val allColumns = receiver.columns() + dataFrames.arguments.flatMap {
            it.resolvedType.findSchemaArgument(isTest)?.getSchema()?.columns() ?: emptyList()
        }
        return PluginDataFrameSchema(allColumns)
    }
}
