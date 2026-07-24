package test

class C {
    var x: Int = 42
}

fun bar(c: C) = foo { c.x = 45 }
