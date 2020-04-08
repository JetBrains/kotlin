fun bar(): String = "bar"

fun main(args: Array<String>) {
    val foo: String? = "foo"
    (foo) ?:<caret> bar()
}
