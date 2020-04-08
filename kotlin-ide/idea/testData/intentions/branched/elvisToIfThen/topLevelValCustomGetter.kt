val a: String?
    get() = ""

fun main(args: Array<String>) {
    a <caret>?: "bar"
}
