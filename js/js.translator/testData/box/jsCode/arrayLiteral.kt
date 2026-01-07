// FILE: a.kt

val jsUndefined = js("(undefined)")

val simple = js("[1, 2, 3]")
val withHoles = js("[1,,,1]")
val withHolesTrailingComma = js("[1,]")
val withHolesLeadingComma = js("[,1]")

// FILE: b.kt
// RECOMPILE

fun box(): String {
    assertArrayEquals(arrayOf(1, 2, 3), simple, "Simple")
    assertArrayEquals(arrayOf(1, jsUndefined, jsUndefined, 1), withHoles, "With holes")
    assertArrayEquals(arrayOf(1), withHolesTrailingComma, "With holes and trailing comma")
    assertArrayEquals(arrayOf(jsUndefined, 1), withHolesLeadingComma, "With holes and leading comma")

    return "OK"
}