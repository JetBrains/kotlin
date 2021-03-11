data open class TestData(val a: Int) {
    fun some() {}
}

class Derived: TestData(1) {
    val field = component1()
}

// TODO: show component1, component2 functions?
// TODO: should equals and hashCode be shown as declared in TestData?

// WITH_INHERITED