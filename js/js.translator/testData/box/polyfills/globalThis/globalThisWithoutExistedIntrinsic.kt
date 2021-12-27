// !LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping
// IGNORE_BACKEND: JS
// FILE: main.js
var isLegacyBackend =
    typeof Kotlin != "undefined" && typeof Kotlin.kotlin != "undefined"

if (!isLegacyBackend) {
    globalThis = undefined
    this["Is Just Created Global This"] = true
}
// FILE: main.kt
external val `Is Just Created Global This`: Boolean

fun box(): String {
    assertEquals(`Is Just Created Global This`, true)
    return "OK"
}
