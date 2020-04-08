// WITH_RUNTIME

data class My(val first: String, val second: Int)

fun foo(list: List<My>) {
    for (<caret>my in list) {
        println(my.second)
    }
}