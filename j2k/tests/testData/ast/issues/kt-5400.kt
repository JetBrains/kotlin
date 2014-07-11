class Base {
    inner class Nested
}

class Derived : Base() {
    var field: Base.Nested
}