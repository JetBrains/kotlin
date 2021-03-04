package serialization.fake_overrides

open class X {
    fun bar() = "Moved"
}

class Y: X() {
}

class B: A() {
    override fun qux() = "Child"
}

class C: A() {
}
