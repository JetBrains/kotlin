package foo

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema

@DataSchema
interface Schema {
    val a: Int
    val b: String
}
