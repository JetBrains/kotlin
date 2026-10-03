// POLYMORPHIC_DATA_SCHEMAS
package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*

@DataSchema
interface UserLike {
    val name: String
    val age: Int
}

fun DataFrame<UserLike>.nameAndAge(): String = rows().joinToString { "${it.name} is ${it.age} years old" }

fun missingColumn() {
    val df = dataFrameOf("name" to columnOf("Alice"))
    df.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>nameAndAge<!>()
}

fun wrongType() {
    val df = dataFrameOf(
        "name" to columnOf("Alice"),
        "age" to columnOf("12"),
    )
    df.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>nameAndAge<!>()
}

fun wrongNullability() {
    val df = dataFrameOf(
        "name" to columnOf("Alice", null),
        "age" to columnOf(12, 13),
    )
    df.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>nameAndAge<!>()
}

fun wrongColumnName() {
    val df = dataFrameOf(
        "userName" to columnOf("Alice"),
        "age" to columnOf(12),
    )
    df.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>nameAndAge<!>()
}

// A nullable column is fine when the schema declares it nullable
@DataSchema
interface MaybeNamed {
    val name: String?
}

fun DataFrame<MaybeNamed>.names(): String = rows().joinToString { it.name.toString() }

fun nullableIsCompatible() {
    val df = dataFrameOf("name" to columnOf("Alice", null))
    df.names()
}
