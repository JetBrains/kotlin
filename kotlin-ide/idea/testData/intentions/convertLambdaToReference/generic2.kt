// WITH_RUNTIME
fun <T> foo(i: Int): T? = null

fun test(list: List<Int>) {
    list.mapNotNull <caret>{ foo<String>(it) }
}