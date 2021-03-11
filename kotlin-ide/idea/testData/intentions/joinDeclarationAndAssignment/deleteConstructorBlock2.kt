// DISABLE-ERRORS
class A(i: Int, j: Int) {
    constructor(i: Int) : this(i, 2) {
        a = 1
    }

    val a<caret>: Int
}