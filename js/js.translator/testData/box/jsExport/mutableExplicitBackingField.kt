// JS_MODULE_KIND: COMMON_JS
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ExplicitBackingFields
// WITH_STDLIB
@file:OptIn(kotlin.js.ExperimentalJsCollectionsApi::class)

import kotlin.js.collections.JsArray
import kotlin.js.collections.JsReadonlyArray

@JsExport
val array: JsReadonlyArray<String>
    field: JsArray<String> = JsArray()

external interface JsResult {
    val value: String
}

@JsModule("lib")
external fun jsBox(): JsResult

fun box(): String {
    val res = jsBox()
    return res.value
}
