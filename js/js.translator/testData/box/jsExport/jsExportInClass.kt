// JS_MODULE_KIND: COMMON_JS

// FILE: api.kt
package api

@JsExport
class A() {
    fun ping() = "ping"
}

@JsExport
class B() {
    @JsName("pong")
    fun ping() = "pong"
}

// we need his class to make sure that there's more than one ping method in existence - due to peculiarities of current namer otherwise test can pass but JsExport won't be actually respected
class C() {
    fun ping() = "pong"
}

// KT-85924

@JsExport
abstract class Base {
    open val size: UInt? = 42u
    fun getSize() = size
}

class Derived : Base() {
    override val size = null
}

@JsExport
class ExportedDerived : Base() {
    override val size = null
}

@JsExport
fun getBase(): Base = Derived()

// FILE: main.kt
external interface JsResult {
    val res: String
}

@JsModule("lib")
external fun jsBox(): JsResult

fun box(): String {
    val res = jsBox().res
    if (res != "pingpong") {
        return "Fail: ${res}"
    }

    return "OK"
}
