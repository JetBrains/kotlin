// COMPILER_ARGUMENTS: -XXLanguage:+TrailingCommas
// FIX: Add line break
// DISABLE-ERRORS

fun a() {
    val (a,
    b,<caret>) = 1 to 2
}