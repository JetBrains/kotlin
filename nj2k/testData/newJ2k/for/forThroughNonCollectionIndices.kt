internal class X {
    fun size(): Int {
        return 5
    }
}

internal class C {
    fun foo(x: X) {
        for (i in 0 until x.size()) {
            print(i)
        }
    }
}