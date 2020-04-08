class D<T>(x: T) {
    companion object {
        val b = D(B())
        val c = D(C())
    }

    var a = A(x)
}