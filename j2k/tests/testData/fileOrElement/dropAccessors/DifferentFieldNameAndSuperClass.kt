public class Base(x: Int) {
    public var x: Int = 42
        protected set

    {
        this.x = x
    }
}

class Derived(b: Base) : Base(b.x)