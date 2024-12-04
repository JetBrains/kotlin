// FIR_IDENTICAL
package test

class A constructor(val x: Int) {
    companion object {
        fun create(x: Int): A = A(x * 2)
    }
}