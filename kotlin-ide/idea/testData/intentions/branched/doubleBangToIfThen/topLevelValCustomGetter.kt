// WITH_RUNTIME
val a: String?
    get() = ""

fun main(args: Array<String>) {
    val x = a<caret>!!
}
