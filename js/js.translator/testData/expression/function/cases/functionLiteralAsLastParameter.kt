package foo

fun f(a: (Int) -> Int) = a(1)

fun box(): Boolean {

    if (f() {
        it + 2
    } != 3) return false

    if (f() { a: Int -> a * 300 } != 300) return false;

    return true
}