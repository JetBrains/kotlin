package serialization.fake_overrides

open class X {
}

class Y: X() {
    fun bar() = "Stale"
}

class B: A() {
}

class C: A() {
    override fun tic() = "Child"
}
