
interface Intf {
  fun v(): Int
}
interface IntfWithProp : Intf {
  val x: Int
}
abstract class Base(p: Int) {
    open protected fun v(): Int? { }
    fun nv() { }
    abstract fun abs(): Int

    internal open val x: Int get() { }
    open var y = 1
    open protected var z = 1
}
class Derived(p: Int) : Base(p), IntfWithProp {
    override fun v() = unknown()
    override val x = 3
    override fun abs() = 0
}
abstract class AnotherDerived(override val x: Int, override val y: Int, override val z: Int) : Base(2) {
    final override fun v() { }
    abstract fun noReturn(s: String)
    abstract val abstractProp: Int
}

private class Private {
    override val overridesNothing: Boolean
        get() = false
}
