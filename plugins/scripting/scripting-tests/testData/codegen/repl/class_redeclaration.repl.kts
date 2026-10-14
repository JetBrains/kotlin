// SNIPPET

class A {
    fun foo() = "Old"
}

// SNIPPET

val oldA = A()

// SNIPPET

class A {
    fun foo() = "New"
}

// SNIPPET

val res1 = oldA.foo()
val res2 = A().foo()
val res3 = oldA.javaClass == A::class.java

// EXPECTED: res1 == Old
// EXPECTED: res2 == New
// EXPECTED: res3 == false
