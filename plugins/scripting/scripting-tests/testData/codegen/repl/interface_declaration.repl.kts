// SNIPPET

interface A

// SNIPPET

interface B : A {
    fun foo() = 45
}

// SNIPPET

val res1 = A::class.java.isAssignableFrom(B::class.java)

// EXPECTED: res1 == true

// SNIPPET

class C : B

val res2 = C().foo()

// EXPECTED: res2 == 45
