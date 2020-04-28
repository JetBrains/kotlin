internal class A // this is a primary constructor
// this is a secondary constructor 1
// end of primary constructor body
@JvmOverloads constructor(p: Int = 1) {
    private val v = 1

    // this is a secondary constructor 2
    constructor(s: String) : this(s.length) {} // end of secondary constructor 2 body
    // end of secondary constructor 1 body
}

internal class B     // this constructor will disappear
// end of constructor body
    (private val x: Int) {
    fun foo() {}
}

internal class CtorComment     /*
     * The magic of comments
     */
// single line magic comments
{
    var myA = "a"
}

internal class CtorComment2  /*
     * The magic of comments
     */
// single line magic comments
