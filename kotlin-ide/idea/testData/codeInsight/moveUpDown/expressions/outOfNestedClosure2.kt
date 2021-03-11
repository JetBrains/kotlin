// MOVE: down
fun foo() {
    val x = run(1, 2) {
        println("bar")
        <caret>println("foo")
    }
}