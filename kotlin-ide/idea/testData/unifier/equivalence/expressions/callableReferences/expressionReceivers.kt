public fun String.bar() = ""

fun foo() {
    val f = if (1 < 2) {
        <selection>"abc"::bar</selection>
    } else {
        "def"::bar
    }
    val ff = if (1 < 2) {
        ("abc")::bar
    } else {
        ("def")::bar
    }
}