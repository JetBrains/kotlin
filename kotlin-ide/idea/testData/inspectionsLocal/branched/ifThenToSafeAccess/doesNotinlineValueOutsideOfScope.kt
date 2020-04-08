fun maybeFoo(): String? {
    return "foo"
}

val x = maybeFoo()

fun main(args: Array<String>) {
    <caret>if (x != null) {
        x.length
    } else {
        null
    }
}
