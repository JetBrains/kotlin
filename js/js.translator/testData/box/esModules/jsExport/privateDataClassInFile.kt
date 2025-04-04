// KT-75772
// ES_MODULES

// FILE: api.kt
@file:JsExport
package api

data class PublicTable(val foo: Int)

private data class PrivateTable(val foo: Int)

fun foo() =
    // Referrence private class to check if NPE happens (KT-75772)
    "OK" + PrivateTable::class.js.asDynamic()["\$metadata$"].simpleName

// FILE: main.kt
external interface JsResult {
    val value: String
    val private: Any?
    val public: Any?
}

@JsModule("./privateDataClassInFile.mjs")
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
