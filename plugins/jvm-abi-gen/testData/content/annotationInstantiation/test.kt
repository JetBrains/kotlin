package test

annotation class A

class Test {
    inline fun annotationInstantiation() = A()
}
