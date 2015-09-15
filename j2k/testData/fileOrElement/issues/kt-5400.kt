// ERROR: Property must be initialized or be abstract
internal open class Base {
    internal inner class Nested
}

internal class Derived : Base() {
    internal var field: Base.Nested
}