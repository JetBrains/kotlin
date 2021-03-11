// PROBLEM: none
fun main(args: Array<String>) {
    val foo = null
    if (foo == null<caret>) {
        null
    }
    else {
        null
    }
}
