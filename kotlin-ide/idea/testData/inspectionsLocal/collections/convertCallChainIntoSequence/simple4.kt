// WITH_RUNTIME

fun test(foo: Foo): List<Int> {
    return foo.getList()
            .<caret>filter { it > 1 }
            .map { it * 2 }
}

class Foo {
    fun getList(): List<Int> = listOf(1, 2, 3)
}