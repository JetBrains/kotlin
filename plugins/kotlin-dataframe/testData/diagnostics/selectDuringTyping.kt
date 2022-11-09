package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.explode

@DataSchema
interface ExplodeSchema {
    val timestamps: List<Int>
}

fun explode(df: DataFrame<ExplodeSchema>) {
    df.explode { <!UNRESOLVED_REFERENCE!>timestamp<!> }
}
