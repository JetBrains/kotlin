// MODULE: common
// MODULE_KIND: Source

// FILE: schema.kt
package org.test

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

@DataSchema
interface MySchema {
    val test: String
}


// FILE: source.kt
package test

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

fun test() {
    val df = dataFrameOf("col" to columnOf(1))
    <caret_context>
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: common

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
df.filter { col == 1 }