// DUMP_IR

// MODULE: common
// MODULE_KIND: Source
// FILE: source.kt
package test

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

fun test() {
    val df = dataFrameOf("name" to listOf("Alice", "Bob"), "age" to listOf(25, 30))
    <caret_context>
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: common

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
df.name