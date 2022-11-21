package org.jetbrains.kotlinx.dataframe

import kotlin.random.Random
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.add
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.convert
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.explode
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.first
import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.api.with

@DataSchema
interface ExplodeSchema {
    val timestamps: List<Int>
}

fun explode(df: DataFrame<ExplodeSchema>) {
    val res = df.explode { timestamps }
    val col: DataColumn<Int> = res.timestamps
}

fun main() {
    val df = dataFrameOf("timestamps")(listOf(100, 113, 140), listOf(400, 410, 453)).cast<ExplodeSchema>()
    val df1 = df.explode { timestamps }
    val timestamps: DataColumn<Int> = df1.timestamps
    timestamps.print()
}
