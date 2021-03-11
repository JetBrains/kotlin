// "Create class 'Foo'" "true"

class A(n: Int)

fun test() = <caret>Foo(abc = 1, ghi = A(2), def = "s")