// PROBLEM: none
fun main(args: Array<String>) {
    val foo: String? = "foo"
    if (null == <caret>null) {
        foo
    }
    else {
        null
    }
}
