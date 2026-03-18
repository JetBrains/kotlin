// KT-75772
// JS_MODULE_KIND: COMMON_JS

// FILE: api.kt
package api

@JsExport
data class PublicTable(val foo: Int)

@JsExport
private data class PrivateTable(val foo: Int)

@JsExport
fun foo() =
    // Referrence private class to check if NPE happens (KT-75772)
    "OK" + PrivateTable::class.js.asDynamic()["\$metadata$"].simpleName

// FILE: main.kt
external interface JsResult {
    val value: String
    val private: Any?
    val public: Any?
}

@JsModule("lib")
external fun jsBox(): JsResult

fun box(): String {
    val res = jsBox()

    if (res.value != "OKPrivateTable") {
        return "Fail: value is ${res.value}"
    }
    if (res.private != null) {
        return "Fail: private is ${res.private}"
    }
    if (res.public == null) {
        return "Fail: public is ${res.public}"
    }

    return "OK"
}
