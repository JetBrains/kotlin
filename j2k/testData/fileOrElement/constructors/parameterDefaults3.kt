package pack

class C(a: Int = 0, b: Int = 0, c: Int = 0, d: Int = 0, e: Int = 0) {

    constructor(a: Int, b: Int, c: Int) : this(b, a, c, 0, 0) {
    }
}
