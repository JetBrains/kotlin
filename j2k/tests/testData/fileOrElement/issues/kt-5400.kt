// ERROR: This type is final, so it cannot be inherited from
// ERROR: Property must be initialized or be abstract
class Base {
    inner class Nested
}

class Derived : Base() {
    var field: Base.Nested
}