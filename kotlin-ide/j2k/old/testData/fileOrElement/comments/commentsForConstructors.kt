internal class A// this is a primary constructor
@JvmOverloads constructor(p: Int = 1) {
    private val v: Int

    init {
        v = 1
    } // end of primary constructor body

    // this is a secondary constructor 2
    constructor(s: String) : this(s.length) {} // end of secondary constructor 2 body
}// this is a secondary constructor 1
// end of secondary constructor 1 body

internal class B// this constructor will disappear
(private val x: Int) // end of constructor body
{

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

/*
     * The magic of comments
     */
// single line magic comments
internal class CtorComment2