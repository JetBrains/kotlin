package foo

class A()

private val doInit = {
    A()
}()

fun box(): String = if (doInit is A) "OK" else "fail"
