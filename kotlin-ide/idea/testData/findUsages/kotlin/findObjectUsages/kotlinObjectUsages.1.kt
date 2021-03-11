package client

import server.O

class Client {
    fun fooBar() {
        println("foo = ${O.foo}")
        val obj = O
        println("length: ${obj.foo.length()}")
    }
}