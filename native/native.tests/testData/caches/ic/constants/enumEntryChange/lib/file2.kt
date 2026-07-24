package test

fun foo(color: Color): String = when (color) {
    Color.RED -> "red_foo"
    Color.BLUE -> "blue_foo"
    Color.YELLOW -> "yellow_foo"
}
