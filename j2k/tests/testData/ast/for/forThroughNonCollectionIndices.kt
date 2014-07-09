class X {
    public fun size(): Int {
        return 5
    }
}

class C {
    fun foo(x: X) {
        for (i in 0..x.size() - 1) {
            System.out.print(i)
        }
    }
}