internal class A // end of primary constructor body
@JvmOverloads constructor(p: Int = 1) {
    private val v = 1

    // this is a secondary constructor 2
    constructor(s: String) : this(s.length) {} // end of secondary constructor 2 body

// this is a secondary constructor 1


    // this is a primary constructor


    // end of secondary constructor 1 body
}

internal class B // end of constructor body
(private val x: Int) {
    fun foo() {}
// this constructor will disappear

}

internal class CtorComment {
    var myA = "a"
/*
     * The magic of comments
     */
    // single line magic comments

}

internal class CtorComment2 /*
     * The magic of comments
     */
// single line magic comments
