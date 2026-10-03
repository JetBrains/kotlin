// POLYMORPHIC_DATA_SCHEMAS
package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*

// A refined call with an incompatible schema in a position with an expected type is reported as a regular type mismatch

@DataSchema
interface PolySchema {
    val age: Int
}

fun take(poly: DataFrame<PolySchema>) {}

fun missingColumn(): DataFrame<PolySchema> {
    return <!RETURN_TYPE_MISMATCH!>dataFrameOf("name" to columnOf("a"))<!>
}

fun wrongType(): DataFrame<PolySchema> = <!RETURN_TYPE_MISMATCH!>dataFrameOf("age" to columnOf("12"))<!>

fun main() {
    val poly: DataFrame<PolySchema> = <!INITIALIZER_TYPE_MISMATCH!>dataFrameOf<!>("name" to columnOf("a"))

    take(<!ARGUMENT_TYPE_MISMATCH!>dataFrameOf("name" to columnOf("a"))<!>)
}
