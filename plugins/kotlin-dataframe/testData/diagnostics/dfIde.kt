package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.add
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.filter

@DataSchema
interface Schema {
    val i: Int
    val fff: String
}

//val DataFrame<Schema>.

fun main(args: Array<String>) {
    val df = dataFrameOf("i")(1, 2, 3).cast<Schema>()
    println(df.i)

    val df1 = df.add("ca") { 423 }
    val res = df1.ca
    df1.filter { it.ca == 12 }

    `Name is evaluated to age`(dataFrameOf("a")(123).cast())
}

interface Cars

fun `Name is evaluated to age`(df: DataFrame<Cars>) {
    val df1 = df.add("age") { 2022 }
    val col = df1.age
    println(col)
}
