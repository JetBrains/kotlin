package foo

class A()

private val doInit = {
    A()
}()

fun box(): Boolean = doInit is A
