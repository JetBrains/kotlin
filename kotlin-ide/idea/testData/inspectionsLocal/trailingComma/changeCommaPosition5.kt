// COMPILER_ARGUMENTS: -XXLanguage:+TrailingCommas
// FIX: Fix comma position

val x = {
        x: Comparable<Comparable<Number>>,
        y: String
        <caret>,->
    val a = 42
}