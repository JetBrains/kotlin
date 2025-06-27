// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// MODULE_KIND: AMD

// FILE: foo.kt
@file:JsModule("foo")
@file:JsNonModule
internal external class StateProvider {
    companion object {
        fun getStateValue(): String
    }
}

// FILE: bar.kt
@JsModule("bar")
@JsNonModule
internal external object Bar

// FILE: main.kt
fun box(): String {
    return StateProvider.getStateValue()
}