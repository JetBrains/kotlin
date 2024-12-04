package lib

class A private constructor(val x: Int) {
    companion object {
        fun create(x: Int): A = A(x * 2)
    }
}
