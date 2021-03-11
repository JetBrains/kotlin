// PROBLEM: none

fun <T> doIt(p: () -> T): T = p()
fun Any.doDo() = Unit

abstract class A {
    abstract fun a()
}

class B : A() {
    override fun a() = doIt {
        null?.doDo()
        Unit<caret>
    }
}

