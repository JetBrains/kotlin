//This is not treated correctly when object is created.

package foo

class B {

    val d = true

    fun f() : Boolean {
      val c = object  {
            fun foo() : Boolean {
                return d
            }
        }
        return c.foo()
    }
}

fun box() : Boolean {
    return B().f()
}