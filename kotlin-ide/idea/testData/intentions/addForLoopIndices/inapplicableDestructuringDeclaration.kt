// IS_APPLICABLE: false
// WITH_RUNTIME
fun test() {
    val list = listOf(Pair("foo", "bar"))
    for ((x, y) <caret>in list) {
    }
}