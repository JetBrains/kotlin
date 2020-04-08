// MOVE: down

fun foo(x: Boolean) {
    <caret>x.let { printn(x) }
    if (x) {

    }
    else {

    }
}
