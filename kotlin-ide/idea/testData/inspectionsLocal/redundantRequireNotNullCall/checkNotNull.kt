// WITH_RUNTIME
fun test(s: String) {
    println(1)
    kotlin.<caret>checkNotNull(s)
    println(2)
}