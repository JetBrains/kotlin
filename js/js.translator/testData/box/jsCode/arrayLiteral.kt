// FILE: a.kt

val jsUndefined = js("undefined")

val simple = js("[1, 2, 3]")
val withHoles = js("[1,,,1]")
val withHolesTrailingComma = js("[1,]")
val withHolesLeadingComma = js("[,1]")
val withInlineSpread = js("[1, ...[2, 3], 4]")
val withInlineSpreadComma = js("[1, ...([3, 2], [2, 3]), 4]")
val withNamedSpread = js("((items) => [1, ...items, 5])([2, 3, 4])")
val withOnlyNamedSpread = js("((items) => [...items])([2, 3, 4])")

// FILE: b.kt
// RECOMPILE

fun box(): String {
    assertArrayEquals(arrayOf(1, 2, 3), simple, "Simple")
    assertArrayEquals(arrayOf(1, jsUndefined, jsUndefined, 1), withHoles, "With holes")
    assertArrayEquals(arrayOf(1), withHolesTrailingComma, "With holes and trailing comma")
    assertArrayEquals(arrayOf(jsUndefined, 1), withHolesLeadingComma, "With holes and leading comma")
    assertArrayEquals(arrayOf(1, 2, 3, 4), withInlineSpread, "With inline spread")
    assertArrayEquals(arrayOf(1, 2, 3, 4), withInlineSpreadComma, "With inline spread inside comma")
    assertArrayEquals(arrayOf(1, 2, 3, 4, 5), withNamedSpread, "With named spread")
    assertArrayEquals(arrayOf(2, 3, 4), withOnlyNamedSpread, "With only named spread")

    return "OK"
}