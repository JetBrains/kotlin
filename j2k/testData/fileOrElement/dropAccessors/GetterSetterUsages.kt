class AAA {
    var x = 42

    fun foo() {
        x = x + 1
    }

    fun bar(other: AAA) {
        other.x = other.x + 1
    }
}

internal class B {
    internal fun foo(a: AAA) {
        a.x = a.x + 1
    }
}
