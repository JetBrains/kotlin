internal class X {
    fun size(): Int {
        return 5
    }
}

internal class C {
    internal fun foo(x: X) {
        for (i in 0..x.size() - 1) {
            print(i)
        }
    }
}