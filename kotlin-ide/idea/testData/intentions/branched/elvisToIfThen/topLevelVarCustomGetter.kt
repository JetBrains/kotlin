var a: String?
    get() = ""
    set(v) {}

fun main(args: Array<String>) {
    a ?:<caret> "bar"
}
