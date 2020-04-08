package client

import server.foo

class Client {
    fun fooBar() {
        println("foo = ${server.foo}")
        println("length: ${server.foo.length()}")
    }
}