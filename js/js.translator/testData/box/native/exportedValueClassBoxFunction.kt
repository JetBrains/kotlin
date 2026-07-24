// TARGET_BACKEND: JS_IR

// FILE: main.kt

var constructorCalls = 0

@JsExport
value class ExportedValue(val value: String) {
    init {
        constructorCalls++
    }
}

external fun externalValue(): ExportedValue

fun box(): String {
    val value = externalValue()
    if (constructorCalls != 0) return "fail"
    return value.value
}

// FILE: external.js
function externalValue() {
    return "OK";
}
