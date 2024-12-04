@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")

import kotlinx.serialization.*
import kotlinx.serialization.json.*

object Instance

val defaultWarn = Json {}
val receiverWarn = Json {encodeDefaults = true}.encodeToString(Instance)
val noWarnFormat = Json {encodeDefaults = true}
val receiverNoWarn = noWarnFormat.encodeToString(Instance)
val defaultNoWarn = Json.encodeToString(Instance)

class SomeContainerClass {
    val memberDefaultWarn = Json {}
    val memberReceiverWarn = Json {encodeDefaults = true}.encodeToString(Instance)
    val memberNoWarnFormat = Json {encodeDefaults = true}
    val memberReceiverNoWarn = noWarnFormat.encodeToString(Instance)
    val memberDefaultNoWarn = Json.encodeToString(Instance)

    fun testDefaultWarnings() {
        Json {}
        Json() {}
        Json {}.encodeToString(Any())
        Json {}.encodeToString(Instance)
        Json { /*some comment*/ }.encodeToString(Instance)
        val localDefaultFormat = Json {}
        Json(Json.Default) {}
        Json(Json) {}
        Json(Json.Default, {})
        Json(builderAction = {})
        Json(builderAction = fun JsonBuilder.() {})
        Json(builderAction = fun JsonBuilder.() = Unit)

        "{}".let {
            Json {}.decodeFromString<Any>(it)
        }
    }

    fun testReceiverWarnings() {
        Json {encodeDefaults = true}.encodeToString(Instance)
        val encoded = Json {encodeDefaults = true}.encodeToString(Instance)
        Json {encodeDefaults = true}.decodeFromString<Any>("{}")
        Json {encodeDefaults = true}.hashCode()
        Json {encodeDefaults = true}.toString()

        Json(noWarnFormat) {encodeDefaults = true}.encodeToString(Instance)
        Json(builderAction = {encodeDefaults = true}).encodeToString(Instance)
        Json(noWarnFormat, {encodeDefaults = true}).encodeToString(Instance)
        Json(builderAction = fun JsonBuilder.() {encodeDefaults = true}).encodeToString(Instance)

        "{}".let {
            Json {encodeDefaults = true}.decodeFromString<Any>(it)
        }
    }

    fun testReceiverNoWarnings() {
        val localFormat = Json {encodeDefaults = true}
        localFormat.encodeToString(Instance)
        localFormat.decodeFromString<Any>("{}")
        localFormat.hashCode()
        localFormat.toString()
    }

    fun testDefaultNoWarnings() {
        val localDefault = Json
        Json.encodeToString(Instance)
        Json.decodeFromString<Any>("{}")
        Json.hashCode()
        Json.toString()
        Json(builderAction = this::builder)
        Json(Json.Default, this::builder)
    }

    private fun builder(builder: JsonBuilder) {
        //now its empty builder
    }
}