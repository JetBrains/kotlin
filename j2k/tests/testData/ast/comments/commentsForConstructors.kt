class A// this is a primary constructor
(p: Int) {
    private val v: Int

    {
        v = 1
    } // end of primary constructor body

    class object {

        // this is a secondary constructor
        fun create(): A {
            val __ = A(1)
            return __
        } // end of secondary constructor body
    }
}

class B// this constructor will disappear
(private val x: Int) // end of constructor body
{

    fun foo() {
    }
}