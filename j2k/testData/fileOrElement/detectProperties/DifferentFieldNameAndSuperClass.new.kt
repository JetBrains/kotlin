open class Base internal constructor(val x: Int)
internal class Derived(b: Base) : Base(b.x)