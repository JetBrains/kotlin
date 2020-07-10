// before function
fun <caret>a() = /* before println */ println(4/*42*/) /* after 1 */ /* after 2 */ /* after 3 */ // after 4

fun test() {
    a()
}