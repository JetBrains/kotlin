// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun test() {
    val a = ArrayList<Int>()
    <caret>for (i in 1..100) {
        a.add(i + 1)
    }
    println(a)
}