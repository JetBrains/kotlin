// ERROR: Property must be initialized or be abstract
internal open class Base {
    internal inner class Nested
}

internal class Derived : Base() {
    var field: Base.Nested
}