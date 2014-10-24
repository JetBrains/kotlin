// ERROR: Property must be initialized or be abstract
open class Base {
    inner class Nested
}

class Derived : Base() {
    var field: Base.Nested
}