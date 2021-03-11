// WITH_RUNTIME
fun foo(list: List<String>) {
    var i = 0
    <caret>for (s in list) {
        println(i)
        val x = s.length * ++i
        if (x > 1000) break
    }
}