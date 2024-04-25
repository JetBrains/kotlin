// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping
// TARGET_BACKEND: JS_IR
// FILE: main.js
this.globalThis = undefined
this["Is Just Created Global This"] = true

// FILE: main.kt
external val `Is Just Created Global This`: Boolean

fun box(): String {
    assertEquals(`Is Just Created Global This`, true)
    return "OK"
}
