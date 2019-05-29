internal class A @JvmOverloads constructor(p: Int = 1) {
    private val v: Int

    // this is a secondary constructor 2
    constructor(s: String) : this(s.length) {} // end of secondary constructor 2 body

// this is a secondary constructor 1


    // this is a primary constructor
    init {
        v = 1
    } // end of primary constructor body


    // end of secondary constructor 1 body
}

internal class B // this constructor will disappear
// end of constructor body
(private val x: Int) {
    fun foo() {}

}

internal class CtorComment {
    var myA: String

    /*
     * The magic of comments
     */
    // single line magic comments
    init {
        myA = "a"
    }
}

internal class CtorComment2 /*
     * The magic of comments
     */
// single line magic comments