package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.type

class FillNulls0 : AbstractInterpreter<FillNullsApproximation>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): FillNullsApproximation {
        return FillNullsApproximation(receiver, columns)
    }
}

class FillNullsApproximation(val schema: PluginDataFrameSchema, val columns: ColumnsResolver)

class UpdateWith0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: FillNullsApproximation by arg()
    val Arguments.expression: TypeApproximation by type()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return convertImpl(receiver.schema, receiver.columns.resolve(receiver.schema).map { it.path.path }, expression)
    }
}
