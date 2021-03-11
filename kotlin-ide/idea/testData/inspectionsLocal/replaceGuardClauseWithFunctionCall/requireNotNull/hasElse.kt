// WITH_RUNTIME
fun test(foo: Int?) {
    <caret>if (foo == null) throw IllegalArgumentException("test") else println(1)
}