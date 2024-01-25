package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.schema.ColumnSchema
import org.jetbrains.kotlinx.dataframe.schema.DataFrameSchema

public val KotlinTypeFacade.toPluginDataFrameSchema: DataFrameSchema.() -> PluginDataFrameSchema get() =  {
    PluginDataFrameSchema(
        columns = columns.map { (name, columnSchema) ->
            when (columnSchema) {
                is ColumnSchema.Value -> {
                    val type = from(columnSchema.type)
                    SimpleCol(name, type)
                    // class Data(val i: Int) // local class
                    // val df = dataFrameOf("col")(Data(1))
                    // val schema = df.schema()
                    // generateInterface(schema)
                }
                is ColumnSchema.Group -> SimpleColumnGroup(name, columnSchema.schema.toPluginDataFrameSchema().columns(), anyRow)
                is ColumnSchema.Frame -> SimpleFrameColumn(name, columnSchema.schema.toPluginDataFrameSchema().columns(), columnSchema.nullable, anyDataFrame)
                else -> TODO()
            }
        }
    )
}
