// PROBLEM: none
fun main(args: Array<String>) {
    val foo: String? = null
    if (foo != null<caret>) {
        foo
    }
    else {

    }
}
