class A

val List<<selection>A</selection>>.foo: (A) -> Int
    get() {
        val a: A? = firstOrNull()
        return { 0 }
    }

fun test() {
    val t = listOf(A()).foo
}