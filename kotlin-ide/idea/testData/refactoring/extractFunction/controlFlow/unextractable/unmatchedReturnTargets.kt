// WITH_RUNTIME
fun foo(a: Int): Int {
    a.let {
        <selection>if (it > 0) return@let it else return@foo -it</selection>
    }
    return 0
}