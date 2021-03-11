// WITH_RUNTIME

var a: String? = "A"
fun main(args: Array<String>) {
    a<caret>?.let { it.length + 1 }
}