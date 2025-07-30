// FIR_DUMP
// DUMP_IR
// WITH_SCHEMA_READER

package org.jetbrains.kotlinx.dataframe.annotations

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.columns.*

@DataSchemaSource
interface Schema

fun test(df: DataFrame<Schema>) {
    val i: DataColumn<Int> = df.id
    val str: DataColumn<String> = df.lastName

    val rowI = df[0].id
    val rowStr = df[0].lastName

    val gr = df.group
    val fr = df.frame

    val str1: DataColumn<String> = df.group.deepGroup.col3

    val df1 = df.convert { i }.with { it.toString() }

    val gr1 = df1.group
    val fr1 = df1.frame
}

fun compile() {
    Schema.read("path/to/file")
}

fun box() = "OK"
