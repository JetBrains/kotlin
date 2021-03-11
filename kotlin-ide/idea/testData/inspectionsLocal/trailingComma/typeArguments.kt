// COMPILER_ARGUMENTS: -XXLanguage:+TrailingCommas
// FIX: Add line break
// DISABLE-ERRORS

fun a() {
    b<<caret>Int,
    >()
}