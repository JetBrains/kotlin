// POLYMORPHIC_DATA_SCHEMAS
package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*

// A refined call in a position with an expected type: the expected type takes part in a subtype check while the call is
// completed, before the compatible `@DataSchema` interfaces are known. They must still be taken into account.

@DataSchema
interface PolySchema {
    val age: Int
}

fun DataFrame<PolySchema>.test() = age

fun take(poly: DataFrame<PolySchema>) {}

fun <T> id(t: T): T = t

fun produce(): DataFrame<PolySchema> {
    return dataFrameOf("age" to columnOf(123))
}

fun produceExpressionBody(): DataFrame<PolySchema> = dataFrameOf("age" to columnOf(123))

fun produceFromBranches(flag: Boolean): DataFrame<PolySchema> =
    if (flag) dataFrameOf("age" to columnOf(1)) else dataFrameOf("age" to columnOf(2), "name" to columnOf("a"))

fun produceFromLambda(): DataFrame<PolySchema> = run { dataFrameOf("age" to columnOf(123)) }

fun produceFromChain(): DataFrame<PolySchema> = dataFrameOf("age" to columnOf(123)).add("name") { "a" }

fun produceThroughGenericCall(): DataFrame<PolySchema> = id(dataFrameOf("age" to columnOf(123)))

fun main() {
    // no expected type
    val df = dataFrameOf("age" to columnOf(123))
    df.test()

    val poly: DataFrame<PolySchema> = dataFrameOf("age" to columnOf(123))
    poly.test()

    var assigned: DataFrame<PolySchema> = poly
    assigned = dataFrameOf("age" to columnOf(123))

    take(dataFrameOf("age" to columnOf(123)))

    val l: List<DataFrame<PolySchema>> = listOf(dataFrameOf("age" to columnOf(123)))

    // the extension properties generated for the refined call keep working
    val withExtraColumn: DataFrame<PolySchema> = dataFrameOf("age" to columnOf(123), "name" to columnOf("a"))
    val name: String = dataFrameOf("age" to columnOf(123), "name" to columnOf("a")).name[0]
}
