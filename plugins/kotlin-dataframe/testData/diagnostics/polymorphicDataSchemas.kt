// POLYMORPHIC_DATA_SCHEMAS
package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*

@DataSchema
interface UserLike {
    val name: String
    val age: Int
}

@DataSchema
interface WithUserGroup {
    val user: UserLike
}

@DataSchema
interface WithUserValue {
    val user: String
}

// An empty schema is satisfied by everything, it is never added as a supertype
@DataSchema
interface Anything

// There is nothing to infer `T` from, so a generic schema is never added as a supertype
@DataSchema
interface Generic<T> {
    val name: T
}

// A marker is a class already, it can't extend a second class
@DataSchema
data class NotAnInterface(val name: String, val age: Int)

fun useSchema(row: DataRow<UserLike>): String = "${row.name} is ${row.age} years old"

fun useGroupSchema(row: DataRow<WithUserGroup>): String = row.user.toString()

fun useValueSchema(row: DataRow<WithUserValue>): String = row.user

// `UserLike` is the only schema added to the marker of this call
fun compatible() {
    val df = dataFrameOf(
        "name" to columnOf("Alice"),
        "age" to columnOf(12),
        "favoriteColor" to columnOf("blue"),
    )
}

// `WithUserGroup` matches: `user` is a column group with the columns of `UserLike` in it.
// `WithUserValue` does not: it expects `user` to be a value column.
fun columnGroup() {
    val df = dataFrameOf(
        "name" to columnOf("Alice"),
        "age" to columnOf(12),
    ).group { name and age }.into("user")
}
