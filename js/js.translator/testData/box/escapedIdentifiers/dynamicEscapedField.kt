// IGNORE_BACKEND: JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

package foo

fun box(): String {
    val a: dynamic = js("{ \"--invalid--property@\": 42 }")
    assertEquals(42, a.`--invalid--property@`)

    return "OK"
}