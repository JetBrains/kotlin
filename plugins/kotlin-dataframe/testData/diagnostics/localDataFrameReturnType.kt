package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.*

@DataSchema
interface MySchema {
    val a: Int
}

class Test {
    private val <!DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE!>df<!> = dataFrameOf("a" to columnOf(42))
    val <!DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE, EXPOSED_PROPERTY_TYPE!>df1<!> = dataFrameOf("a" to columnOf(42))
    private val df2 = dataFrameOf("a" to columnOf(42)).cast<MySchema>()
}