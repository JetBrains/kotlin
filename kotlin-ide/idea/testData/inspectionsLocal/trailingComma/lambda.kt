// COMPILER_ARGUMENTS: -XXLanguage:+TrailingCommas
// FIX: Add line break

fun a() {
    val a = { a: Int,
    b: Int, <caret>->

    }
}