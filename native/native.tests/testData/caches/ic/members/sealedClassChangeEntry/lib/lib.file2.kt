package test

fun foo(color: Color): String = when (color) {
    is Red -> "${color.code} foo"
    is Blue -> "${color.code} foo"
}
