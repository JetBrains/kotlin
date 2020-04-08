interface T {
    val foo: Int
}

val f = fun <caret>T.(): Int {
    return foo
}