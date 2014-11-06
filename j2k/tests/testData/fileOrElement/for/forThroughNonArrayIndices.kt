class X {
    public var length: Int = 5
}

class C {
    fun foo(x: X) {
        for (i in 0..x.length - 1) {
            System.out.print(i)
        }
    }
}