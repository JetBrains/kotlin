package dataframe

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class MyRecordModifiedStep2(
    val group: Group,
    val str: kotlin.String
) {
    @DataSchema
    data class Group(
        val a: kotlin.String,
        val length: kotlin.Int
    )
}

@DataSchema
data class MyRecordModified(
    val group: Group
) {
    @DataSchema
    data class Group(
        val a: kotlin.String,
        val length: kotlin.Int
    )
}

@DataSchema
data class Group1(
    val a: kotlin.String,
)

@DataSchema
data class Group2(
    val a: kotlin.String,
)

fun box(): String {
    val df = dataFrameOf(MyRecordModified(MyRecordModified.Group("a", 123)))
    // need to trigger JVM classloading so duplicated signature error can appear
    df.group.a
    return "OK"
}
