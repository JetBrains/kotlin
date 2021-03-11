// MOVE: down
fun foo() {
    <caret>println("foo")
    val x = run(1, 2) {
        x ->
        println("bar")
    }
}