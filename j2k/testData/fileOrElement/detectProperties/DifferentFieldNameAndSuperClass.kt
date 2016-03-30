open class Base internal constructor(x: Int) {
    var x = 42

    init {
        this.x = x
    }
}

internal class Derived(b: Base) : Base(b.x)
