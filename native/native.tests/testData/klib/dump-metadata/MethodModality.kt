// FIR_IDENTICAL
interface Interface {
    fun interfaceFun()
}

abstract class AbstractClass: Interface {
    override fun interfaceFun() {}
    abstract fun abstractFun()
}

open class OpenClass: AbstractClass() {
    override fun abstractFun() {}
    open fun openFun1() {}
    open fun openFun2() {}
    fun finalFun() {}
}

class FinalClass: OpenClass() {
    override fun openFun1() {}
    final override fun openFun2() {}
}