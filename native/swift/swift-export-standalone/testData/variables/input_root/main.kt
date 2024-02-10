package namespace.main

import namespace.*

val foo: Int get() = 10

var bar: Int = 20

fun foobar(param: Int): Int = foo + bar + param + baz
