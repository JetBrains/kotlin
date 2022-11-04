package foo

import org.jetbrains.dataframe.annotations.DataSchema
import org.jetbrains.dataframe.DataFrameBase

@org.jetbrains.dataframe.annotations.DataSchema
interface Schema {
    val a: Int
    val b: String
}