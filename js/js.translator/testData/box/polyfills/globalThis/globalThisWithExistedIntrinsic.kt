// !LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping
// IGNORE_BACKEND: JS
// FILE: main.js
globalThis = { "Is Just Created Global This": true }

// FILE: main.kt
external val `Is Just Created Global This`: Boolean

fun box(): String {
    assertEquals(`Is Just Created Global This`, true)
    return "OK"
}
