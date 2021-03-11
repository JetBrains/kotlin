package test

class A {
    fun /*rename*/namedFunA() {}
    fun namedFunB() {}

    fun useNames() {
        namedFunA()
        namedFunB()
    }
}