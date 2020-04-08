fun <T> doSomething(a: T) {}

val a: String? = "A"
fun main(args: Array<String>) {
    doSomething(a?.<caret>length)
}
