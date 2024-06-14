@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json as JsonImportAlias

object Instance

val defaultWarn = JsonImportAlias {}
val receiverWarn = JsonImportAlias {encodeDefaults = true}.encodeToString(Instance)
val noWarnFormat = JsonImportAlias {encodeDefaults = true}
val receiverNoWarn = noWarnFormat.encodeToString(Instance)
val defaultNoWarn = JsonImportAlias.encodeToString(Instance)

class SomeContainerClass {
    val memberDefaultWarn = JsonImportAlias {}
    val memberReceiverWarn = JsonImportAlias {encodeDefaults = true}.encodeToString(Instance)
    val memberNoWarnFormat = JsonImportAlias {encodeDefaults = true}
    val memberReceiverNoWarn = noWarnFormat.encodeToString(Instance)
    val memberDefaultNoWarn = JsonImportAlias.encodeToString(Instance)

    fun testDefaultWarnings() {
        JsonImportAlias {}
        JsonImportAlias() {}
        JsonImportAlias {}.encodeToString(Any())
        JsonImportAlias {}.encodeToString(Instance)
        JsonImportAlias { /*some comment*/ }.encodeToString(Instance)
        val localDefaultFormat = JsonImportAlias {}
        JsonImportAlias(JsonImportAlias.Default) {}
        JsonImportAlias(JsonImportAlias) {}
        JsonImportAlias(JsonImportAlias.Default, {})
        JsonImportAlias(builderAction = {})
        JsonImportAlias(builderAction = fun JsonBuilder.() {})
        JsonImportAlias(builderAction = fun JsonBuilder.() = Unit)

        "{}".let {
            JsonImportAlias {}.decodeFromString<Any>(it)
        }
    }

    fun testReceiverWarnings() {
        JsonImportAlias {encodeDefaults = true}.encodeToString(Instance)
        val encoded = JsonImportAlias {encodeDefaults = true}.encodeToString(Instance)
        JsonImportAlias {encodeDefaults = true}.decodeFromString<Any>("{}")
        JsonImportAlias {encodeDefaults = true}.hashCode()
        JsonImportAlias {encodeDefaults = true}.toString()

        JsonImportAlias(noWarnFormat) {encodeDefaults = true}.encodeToString(Instance)
        JsonImportAlias(builderAction = {encodeDefaults = true}).encodeToString(Instance)
        JsonImportAlias(noWarnFormat, {encodeDefaults = true}).encodeToString(Instance)
        JsonImportAlias(builderAction = fun JsonBuilder.() {encodeDefaults = true}).encodeToString(Instance)

        "{}".let {
            JsonImportAlias {encodeDefaults = true}.decodeFromString<Any>(it)
        }
    }

    fun testReceiverNoWarnings() {
        val localFormat = JsonImportAlias {encodeDefaults = true}
        localFormat.encodeToString(Instance)
        localFormat.decodeFromString<Any>("{}")
        localFormat.hashCode()
        localFormat.toString()
    }

    fun testDefaultNoWarnings() {
        val localDefault = JsonImportAlias
        JsonImportAlias.encodeToString(Instance)
        JsonImportAlias.decodeFromString<Any>("{}")
        JsonImportAlias.hashCode()
        JsonImportAlias.toString()
        JsonImportAlias(builderAction = this::builder)
        JsonImportAlias(JsonImportAlias.Default, this::builder)
    }

    private fun builder(builder: JsonBuilder) {
        //now its empty builder
    }
}