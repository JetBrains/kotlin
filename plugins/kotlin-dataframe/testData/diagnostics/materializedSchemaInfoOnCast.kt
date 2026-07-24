// RENDER_DIAGNOSTIC_ARGUMENTS
// DUMP_SCHEMAS

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val <!PROPERTY_SCHEMA("city: String")!>cities<!> = <!FUNCTION_CALL_SCHEMA("city: Stringcoordinates: Pair<String, String>")!>dataFrameOf<!>(
        "city" to columnOf("London", "Seoul"),
        "coordinates" to columnOf("51.5074" to "-0.1278", "37.5665" to "126.9780")
    ).<!FUNCTION_CALL_SCHEMA("city: String")!>cast<!><<!MATERIALIZED_SCHEMA_INFO("@DataSchemadata class Cities(    val city: String,    val coordinates: Pair<String, String>,)")!>Cities<!>>()

    // trigger codegen
    <!PROPERTY_ACCESS_SCHEMA("city: String")!>cities<!>.city

    return "OK"
}

@DataSchema
data class Cities(val city: String)
