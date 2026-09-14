// RENDER_DIAGNOSTIC_ARGUMENTS

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val cities = dataFrameOf(
        "city" to columnOf("London", "Seoul"),
    )

    cities.<!MATERIALIZED_SCHEMA_ON_CAST("@DataSchemadata class MySchema(    val city: String,)")!>cast<!><Context.MySchema>()

    return "OK"
}

class Context {
    @DataSchema
    class MySchema
}
