// PROBLEM: none
// SKIP_ERRORS_BEFORE
// SKIP_ERRORS_AFTER

class Point {
    val x: Int

    constructor(x: Int) {
        x = <caret>x
    }
}