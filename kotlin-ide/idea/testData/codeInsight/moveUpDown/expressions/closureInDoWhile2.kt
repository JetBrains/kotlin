// MOVE: up
fun foo(i: Int) {
    do {
        println(i)
    } while (i in run { 1..2 })
    <caret>run {
    }
}