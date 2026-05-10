// RENDER_DIAGNOSTIC_ARGUMENTS

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val cities = dataFrameOf(
        "city" to columnOf("London", "Seoul"),
        "coordinates" to columnOf("51.5074" to "-0.1278", "37.5665" to "126.9780")
    )
        .add("NameCollision") { 1 }
        .group { coordinates }.into("name_collision")
        .add("frame") { dataFrameOf("a" to columnOf(4124142)) }

    cities.<!MATERIALIZED_SCHEMA_INFO("@DataSchemadata class B(    val city: String,    val name_collision: NameCollision1,    val NameCollision: Int,    val frame: List<Frame>,) {    @DataSchema    data class NameCollision1(        val coordinates: Pair<String, String>,    )    @DataSchema    data class Frame(        val a: Int,    )}")!>cast<!><B>()

    cities.<!MATERIALIZED_SCHEMA_INFO("@DataSchemainterface I {    val city: String    val name_collision: NameCollision1    val NameCollision: Int    val frame: List<Frame>    @DataSchema    interface NameCollision1 {        val coordinates: Pair<String, String>    }    @DataSchema    interface Frame {        val a: Int    }}")!>cast<!><I>()
    return "OK"
}

@DataSchema
class B

@DataSchema
interface I
