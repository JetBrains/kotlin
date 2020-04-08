package test

class A {
    companion object {
        fun Int.extFoo(n: Int) {}

        val Int.extBar: Int get() = 1
    }

}