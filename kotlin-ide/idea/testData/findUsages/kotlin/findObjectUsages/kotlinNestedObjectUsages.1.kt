package b

import a.A.O
import a.A

class Client {
    fun fooBar() {
        val a = A()

        println("foo = ${a.O.foo}")
        val obj = a.O
        println("length: ${obj.foo.length}")
    }
}