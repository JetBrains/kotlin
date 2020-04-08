fun <T> doSomething(a: T) {}

fun main(args: Array<String>) {
    val a: String? = "A"
    doSomething(a?.<caret>length)
}
