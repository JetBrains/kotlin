// IGNORE_BACKEND: JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

package foo

fun _my_fn(a: Int): Int { return a }
fun `my fn`(): Int { return 42 }

fun box(): String {
    val fn1 = ::_my_fn
    val fn2 = ::`my fn`

    assertEquals("_my_fn", fn1.name)
    assertEquals("my fn", fn2.name)

    assertEquals(23, _my_fn(23))
    assertEquals(42, `my fn`())

    return "OK"
}