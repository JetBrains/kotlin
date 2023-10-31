package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.plugin.*

interface Schema

fun test() {
    materialize<Schema>("name")
}
