// DUMP_IR

import org.jetbrains.kotlin.specialization.Monomorphic

open class A
class B: A()

abstract class Base {
    abstract fun <@Monomorphic T: A> monomorphic(x: T): T
    abstract fun notMonomorphic()
}

class Derived: Base() {
    override fun <@Monomorphic T: A> monomorphic(x: T): T {
        return x
    }

    override fun notMonomorphic() {}
}

// (T1 -> B, T2 -> Derived)
fun <@Monomorphic T1: A, @Monomorphic T2: Base> test(a: T1, base: T2) {
    base.notMonomorphic() // => Derived::notMonomorphic
    base.monomorphic(a) // => Derived::monomorphic|T=A|
}

fun box(): String {
    test(B(), Derived())
    return "OK"
}
