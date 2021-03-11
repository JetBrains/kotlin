fun foo(/*rename*/a: Int, b: String) {
}

fun main(args: Array<String>) {
    foo(b = "!", a = 333)
}
