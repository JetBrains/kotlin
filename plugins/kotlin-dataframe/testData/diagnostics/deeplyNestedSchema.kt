package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*

fun main() {
    val df = DataFrame.readJsonDefault("/home/nikitak/Downloads/output_file.json")
    df.explode { component.<!SYNTAX!><!> }
}
