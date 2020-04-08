// OPTION: 1
fun foo(n: Int) {
    var m = n
    <caret>do {
        m--
        println(m)
    } while (m > 0)
}
