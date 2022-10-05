// DUMP_IR

import org.jetbrains.kotlin.specialization.Monomorphic

open class Base
class Derived: Base()

abstract class A {
    abstract fun <@Monomorphic T: Base> f(x: T)
}

abstract class B: A()

class C: B() {
    override fun <@Monomorphic T : Base> f(x: T) {}
}


fun box(): String {
    val derived = Derived()
    val c = C()
    c.f(derived)
    return "OK"
}
