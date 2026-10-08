// POLYMORPHIC_DATA_SCHEMAS
// DUMP_IR

// MODULE: common
// MODULE_KIND: Source
// FILE: source.kt
package test

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*

@DataSchema
interface UserLike {
    val name: String
    val age: Int
}

fun DataRow<UserLike>.describe(): String = "$name is $age years old"

fun test() {
    val df = dataFrameOf(
        "name" to columnOf("Alice", "John"),
        "age" to columnOf(12, 13),
        "favoriteColor" to columnOf("blue", "yellow"),
    )
    <caret_context>
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: common

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
df[0].describe()
