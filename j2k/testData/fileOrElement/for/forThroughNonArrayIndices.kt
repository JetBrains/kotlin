internal class X {
    var length = 5
}

internal class C {
    internal fun foo(x: X) {
        for (i in 0..x.length - 1) {
            print(i)
        }
    }
}