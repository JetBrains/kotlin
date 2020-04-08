fun foo() {
    val f: (Int) -> Int = { x -> x }
    val ff: (Int) -> Int = <caret>f
}
