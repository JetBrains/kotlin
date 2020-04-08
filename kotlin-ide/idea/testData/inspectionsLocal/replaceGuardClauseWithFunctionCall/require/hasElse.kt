// WITH_RUNTIME
fun test(flag: Boolean) {
    <caret>if (!flag) throw IllegalArgumentException() else println(1)
}