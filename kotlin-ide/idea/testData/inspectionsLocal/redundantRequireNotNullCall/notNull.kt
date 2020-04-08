// WITH_RUNTIME
fun test(s: String) {
    println(1)
    <caret>requireNotNull(s)
    println(2)
}