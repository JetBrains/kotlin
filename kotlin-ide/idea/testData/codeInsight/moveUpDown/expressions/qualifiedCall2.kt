// MOVE: up

fun foo(x: Boolean) {
    if (x) {

    }
    else {

    }
    <caret>x.let { printn(x) }
}
