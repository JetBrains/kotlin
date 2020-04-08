// "Remove parameter 'x'" "true"

fun Int.f(<caret>x: Int) {

}

fun test() {
    1.f(2)
}