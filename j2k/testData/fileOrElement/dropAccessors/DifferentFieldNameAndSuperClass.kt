public open class Base(x: Int) {
    public var x: Int = 42
        protected set

    init {
        this.x = x
    }
}

class Derived(b: Base) : Base(b.x)