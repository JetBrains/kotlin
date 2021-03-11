// "Change return type of base function 'A.foo' to 'Long'" "true"
interface A {
    fun foo(): Int
}

interface B {
    fun foo(): Number
}

interface C : A, B {
    override fun foo(): Long<caret>
}
/* FIR_COMPARISON */