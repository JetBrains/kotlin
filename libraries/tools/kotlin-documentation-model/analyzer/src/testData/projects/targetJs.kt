// JS-specific declarations: external declarations, @JsName, @JsExport, and dynamic types.

// MODULE: jsMain
// PLATFORM: JS
// FILE: pkg/Js.kt
@file:OptIn(ExperimentalJsExport::class)

package pkg

import kotlin.js.Promise

external interface JsOptions {
    var title: String
    var count: Int?
}

external class JsDate(milliseconds: Number) {
    fun getTime(): Double
}

external fun parseInt(text: String, radix: Int): Int

@JsExport
class JsGreeter(val name: String) {
    @JsName("greetWith")
    fun greet(prefix: String): String = prefix + name

    fun later(): Promise<String> = Promise.resolve(name)
}

fun toDynamic(value: Any): dynamic = value.asDynamic()
