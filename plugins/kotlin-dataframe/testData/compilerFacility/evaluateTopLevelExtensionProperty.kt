// DUMP_IR

// MODULE: common
// MODULE_KIND: Source
// FILE: source.kt
package test

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

@DataSchema
interface MySchema {
    val i: Int
}

fun test() {
    val df = dataFrameOf("i" to columnOf(12)).cast<MySchema>()
    <caret_context>
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: common

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
df.i