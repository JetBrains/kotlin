// MOVE: up
fun foo() {
    val x = run(1, 2) {
        x ->
        <caret>println("foo")
        println("bar")
    }
}