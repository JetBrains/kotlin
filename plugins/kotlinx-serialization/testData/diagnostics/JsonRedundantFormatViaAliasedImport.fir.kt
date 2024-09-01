@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json as JsonImportAlias

object Instance

val defaultWarn = <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias {}<!>
val receiverWarn = <!JSON_FORMAT_REDUNDANT!>JsonImportAlias {encodeDefaults = true}<!>.encodeToString(Instance)
val noWarnFormat = JsonImportAlias {encodeDefaults = true}
val receiverNoWarn = noWarnFormat.encodeToString(Instance)
val defaultNoWarn = JsonImportAlias.encodeToString(Instance)

class SomeContainerClass {
    val memberDefaultWarn = <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias {}<!>
    val memberReceiverWarn = <!JSON_FORMAT_REDUNDANT!>JsonImportAlias {encodeDefaults = true}<!>.encodeToString(Instance)
    val memberNoWarnFormat = JsonImportAlias {encodeDefaults = true}
    val memberReceiverNoWarn = noWarnFormat.encodeToString(Instance)
    val memberDefaultNoWarn = JsonImportAlias.encodeToString(Instance)

    fun testDefaultWarnings() {
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias {}<!>
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias() {}<!>
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias {}<!>.encodeToString(Any())
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias {}<!>.encodeToString(Instance)
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias { /*some comment*/ }<!>.encodeToString(Instance)
        val localDefaultFormat = <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias {}<!>
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias(JsonImportAlias.Default) {}<!>
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias(JsonImportAlias) {}<!>
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias(JsonImportAlias.Default, {})<!>
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias(builderAction = {})<!>
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias(builderAction = fun JsonBuilder.() {})<!>
        <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias(builderAction = fun JsonBuilder.() = Unit)<!>

        "{}".let {
            <!JSON_FORMAT_REDUNDANT_DEFAULT!>JsonImportAlias {}<!>.decodeFromString<Any>(it)
        }
    }

    fun testReceiverWarnings() {
        <!JSON_FORMAT_REDUNDANT!>JsonImportAlias {encodeDefaults = true}<!>.encodeToString(Instance)
        val encoded = <!JSON_FORMAT_REDUNDANT!>JsonImportAlias {encodeDefaults = true}<!>.encodeToString(Instance)
        <!JSON_FORMAT_REDUNDANT!>JsonImportAlias {encodeDefaults = true}<!>.decodeFromString<Any>("{}")
        <!JSON_FORMAT_REDUNDANT!>JsonImportAlias {encodeDefaults = true}<!>.hashCode()
        <!JSON_FORMAT_REDUNDANT!>JsonImportAlias {encodeDefaults = true}<!>.toString()

        <!JSON_FORMAT_REDUNDANT!>JsonImportAlias(noWarnFormat) {encodeDefaults = true}<!>.encodeToString(Instance)
        <!JSON_FORMAT_REDUNDANT!>JsonImportAlias(builderAction = {encodeDefaults = true})<!>.encodeToString(Instance)
        <!JSON_FORMAT_REDUNDANT!>JsonImportAlias(noWarnFormat, {encodeDefaults = true})<!>.encodeToString(Instance)
        <!JSON_FORMAT_REDUNDANT!>JsonImportAlias(builderAction = fun JsonBuilder.() {encodeDefaults = true})<!>.encodeToString(Instance)

        "{}".let {
            <!JSON_FORMAT_REDUNDANT!>JsonImportAlias {encodeDefaults = true}<!>.decodeFromString<Any>(it)
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