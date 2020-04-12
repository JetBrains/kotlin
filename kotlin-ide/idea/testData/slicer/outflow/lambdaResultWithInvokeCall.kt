// FLOW: OUT

fun test() {
    val x = foo { <caret>1 }
}

fun foo(callback: () -> Int): Int {
    return callback.invoke()
}