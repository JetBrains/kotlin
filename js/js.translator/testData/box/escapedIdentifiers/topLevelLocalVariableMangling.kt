// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

package foo

val _my_invalid_variable = 23
val `my@invalid variable` = 42

fun box(): String {
    assertEquals(23, _my_invalid_variable)
    assertEquals(42, `my@invalid variable`)

    return "OK"
}