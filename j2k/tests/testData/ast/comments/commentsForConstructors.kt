// this is a secondary constructor
fun A(): A {
    return A(1)
} // end of secondary constructor body

class A// this is a primary constructor
(p: Int) {
    private val v: Int

    {
        v = 1
    } // end of primary constructor body
}

class B// this constructor will disappear
(private val x: Int) // end of constructor body
{

    fun foo() {
    }
}