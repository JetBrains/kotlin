// WITH_RUNTIME

data class My(val first: String, val second: Int, val third: Boolean)

fun foo(list: List<My>) {
    list.forEach { my<caret> ->
        println(my.second)
        println(my.third)
    }
}