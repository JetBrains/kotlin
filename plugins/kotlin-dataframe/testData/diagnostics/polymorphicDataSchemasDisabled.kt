// The POLYMORPHIC_DATA_SCHEMAS directive is intentionally absent: the feature is opt-in,
// without it the marker of `dataFrameOf` keeps its single generated supertype.
package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*

@DataSchema
interface UserLike {
    val name: String
    val age: Int
}

fun DataFrame<UserLike>.nameAndAge(): String = rows().joinToString { "${it.name} is ${it.age} years old" }

fun test() {
    val df = dataFrameOf(
        "name" to columnOf("Alice"),
        "age" to columnOf(12),
    )
    df.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>nameAndAge<!>()
    df.cast<UserLike>().nameAndAge()
}
