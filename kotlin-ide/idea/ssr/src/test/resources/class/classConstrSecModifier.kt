<warning descr="SSR">class A(val b: Int) {
    var c: String? = null

    private constructor(b: Int, c: String) : this(b) {
        this.c = c
    }
}</warning>

class B(val b: Int) {
    var c: String? = null

    constructor(b: Int, c: String) : this(b) {
        this.c = c
    }
}