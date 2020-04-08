fun main(args: Array<String>) {
    val str: String? = ""

    when(str) {
        null -> test("")
        else -> <caret>test(str)
    }
}

fun test(s: String) = 1