package dependency

class D {
    inner class Inner {
        inner class Inner
    }
    open class Nested {
        class Nested
    }

    companion object {
        class NestedInClassObject
    }
}
