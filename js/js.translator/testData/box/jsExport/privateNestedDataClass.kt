// KT-70987
// MODULE_KIND: COMMON_JS

// FILE: api.kt
package api

@JsExport
open class TableDriver {
    data class PublicTable(val foo: Int)
    private data class PrivateTable(val foo: Int)

    fun foo() = "OK"
}

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

    if (res.value != "OK") {
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
