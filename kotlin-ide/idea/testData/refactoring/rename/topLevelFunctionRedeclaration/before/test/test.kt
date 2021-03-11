package test

fun /*rename*/namedFunA() {}
fun namedFunB() {}

fun useNames() {
    namedFunA()
    namedFunB()
}