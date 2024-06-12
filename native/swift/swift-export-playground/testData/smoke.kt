fun foo() { println("hello") }

class A(
    val boolProp: Boolean,
    val intProp: Int,
    val floatProp: Float,
    val refProp: B,
)

class B

class C {
    class D {
        class E {

        }
    }

    fun method(): D.E = D.E()
}

typealias CDE = C.D.E