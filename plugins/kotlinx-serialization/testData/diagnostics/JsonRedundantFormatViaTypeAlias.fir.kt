@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")

import kotlinx.serialization.*
import kotlinx.serialization.json.*

typealias JsonTypeAlias = Json

object Instance

class SomeContainerClass {
    fun testDefaultWarnings() {
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>Json(JsonTypeAlias) {}<!>
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>Json(JsonTypeAlias, {})<!>
    }

    fun testDefaultNoWarnings() {
        val localDefault = JsonTypeAlias
        JsonTypeAlias.encodeToString(Instance)
        JsonTypeAlias.decodeFromString<Any>("{}")
        JsonTypeAlias.hashCode()
        JsonTypeAlias.toString()
        Json(JsonTypeAlias, this::builder)
    }

    private fun builder(builder: JsonBuilder) {
        //now its empty builder
    }
}