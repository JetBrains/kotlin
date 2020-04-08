// MOVE: up
fun foo() {
    run(1, 2) {
        x ->
        <caret>println("foo")
        println("bar")
    }
}