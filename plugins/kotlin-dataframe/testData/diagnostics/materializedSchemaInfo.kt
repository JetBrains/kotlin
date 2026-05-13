// RENDER_DIAGNOSTIC_ARGUMENTS
// DUMP_SCHEMAS

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val cities = <!FUNCTION_CALL_SCHEMA("city: String")!>dataFrameOf<!>("city" to columnOf("London", "Seoul"))
        .<!MATERIALIZED_SCHEMA_INFO("@DataSchemadata class Cities(    val city: String,)")!>schema<!>()

    <!FUNCTION_CALL_SCHEMA("city: String")!>dataFrameOf<!>("city" to columnOf("London", "Seoul"))
        .<!MATERIALIZED_SCHEMA_INFO("@DataSchemadata class DataFrameOf_29(    val city: String,)")!>schema<!>()

    return "OK"
}
