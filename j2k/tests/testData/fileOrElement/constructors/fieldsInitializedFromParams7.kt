// ERROR: Property must be initialized or be abstract
class C(x: Any, b: Boolean) {
    public var x: Any

    {
        if (b) {
            this.x = x
        }
    }
}