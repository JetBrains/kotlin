// MOVE: down

fun foo(x: Boolean) {
    val p = <caret>if (x) {
        0
    }
    else {
        1
    }
    if (x) {

    }
}
