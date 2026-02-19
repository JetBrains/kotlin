// SNIPPET

// KT-75589

val foo = "OK"

open class A {
    fun bar() = foo
}

class B1 : A()

val rv1 = B1().bar()

// EXPECTED: rv1 == OK

// SNIPPET

class B2 : A()

val rv2 = B2().bar()

// EXPECTED: rv2 == OK


