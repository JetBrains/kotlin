// PROBLEM: none

fun foo(a: Boolean, b: Boolean, c: Boolean) {
    <caret>if (a) {

    }
    else if (b && c) {

    }
    else if (!c) {

    }
    else {

    }
}