// WITH_RUNTIME
fun test(list: List<Any>) {
    list.mapNotNull {
        it as? String ?: return@mapNotNull null<caret>
        foo(it)
    }
}

fun foo(x: String) = 1