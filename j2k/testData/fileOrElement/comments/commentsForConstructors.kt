class A// this is a primary constructor
(p: Int = 1) {
    private val v: Int

    {
        v = 1
    } // end of primary constructor body

    // this is a secondary constructor 2
    constructor(s: String) : this(s.length()) {
    } // end of secondary constructor 2 body
}// this is a secondary constructor 1
// end of secondary constructor 1 body

class B// this constructor will disappear
(private val x: Int) // end of constructor body
{

    fun foo() {
    }
}